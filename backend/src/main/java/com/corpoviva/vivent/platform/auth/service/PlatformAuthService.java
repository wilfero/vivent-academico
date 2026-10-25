package com.corpoviva.vivent.platform.auth.service;

import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.platform.audit.service.PlatformAuditService;
import com.corpoviva.vivent.platform.config.PlatformProperties;
import com.corpoviva.vivent.platform.entity.PlatformSessionEntity;
import com.corpoviva.vivent.platform.entity.PlatformUserEntity;
import com.corpoviva.vivent.platform.entity.PlatformUserStatus;
import com.corpoviva.vivent.platform.repository.PlatformUserRepository;
import com.corpoviva.vivent.platform.security.PlatformPrincipal;
import com.corpoviva.vivent.platform.session.service.PlatformSessionService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformAuthService {

	private final PlatformUserRepository platformUserRepository;
	private final PlatformSessionService platformSessionService;
	private final PlatformAuditService platformAuditService;
	private final PlatformProperties platformProperties;
	private final PasswordEncoder passwordEncoder;

	public PlatformAuthService(
		PlatformUserRepository platformUserRepository,
		PlatformSessionService platformSessionService,
		PlatformAuditService platformAuditService,
		PlatformProperties platformProperties,
		PasswordEncoder passwordEncoder
	) {
		this.platformUserRepository = platformUserRepository;
		this.platformSessionService = platformSessionService;
		this.platformAuditService = platformAuditService;
		this.platformProperties = platformProperties;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public LoginResult login(String username, String password, String ipAddress, String userAgent) {
		String normalizedUsername = username.trim();
		PlatformUserEntity user = platformUserRepository.findByUsername(normalizedUsername).orElse(null);

		if (user == null) {
			throw ApiException.unauthorized(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
		}

		if (isLocked(user)) {
			throw ApiException.forbidden(ErrorCode.PLATFORM_ACCOUNT_LOCKED, "Platform account is temporarily locked");
		}

		if (user.getStatus() != PlatformUserStatus.ACTIVE) {
			throw ApiException.forbidden(ErrorCode.USER_INACTIVE, "Platform account is not active");
		}

		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			registerFailedAttempt(user);
			throw ApiException.unauthorized(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
		}

		user.setFailedAttempts(0);
		user.setLockedUntil(null);
		user.setLastLoginAt(Instant.now());

		PlatformSessionService.IssuedSession issuedSession = platformSessionService.createSession(
			user.getId(),
			ipAddress,
			userAgent
		);

		platformAuditService.log(user.getId(), "LOGIN", "platform_user", user.getId(), null, null, ipAddress, userAgent);

		PlatformPrincipal principal = new PlatformPrincipal(
			user.getId(),
			user.getUsername(),
			user.getDisplayName(),
			issuedSession.expiresAt()
		);

		return new LoginResult(principal, issuedSession.token());
	}

	@Transactional
	public PlatformPrincipal authenticateToken(String token) {
		PlatformSessionEntity session = platformSessionService.findValidSession(token)
			.orElseThrow(() -> ApiException.unauthorized(ErrorCode.AUTHENTICATION_REQUIRED, "Invalid or expired platform session"));

		PlatformUserEntity user = platformUserRepository.findById(session.getPlatformUserId())
			.orElseThrow(() -> ApiException.unauthorized(ErrorCode.AUTHENTICATION_REQUIRED, "Platform account not found"));

		if (user.getStatus() != PlatformUserStatus.ACTIVE) {
			throw ApiException.forbidden(ErrorCode.USER_INACTIVE, "Platform account is not active");
		}

		if (isLocked(user)) {
			throw ApiException.forbidden(ErrorCode.PLATFORM_ACCOUNT_LOCKED, "Platform account is temporarily locked");
		}

		return new PlatformPrincipal(user.getId(), user.getUsername(), user.getDisplayName(), session.getExpiresAt());
	}

	@Transactional
	public void logout(String token, UUID platformUserId, String ipAddress, String userAgent) {
		if (token != null) {
			platformSessionService.revoke(token);
		}

		if (platformUserId != null) {
			platformAuditService.log(platformUserId, "LOGOUT", "platform_user", platformUserId, null, null, ipAddress, userAgent);
		}
	}

	private boolean isLocked(PlatformUserEntity user) {
		if (user.getLockedUntil() == null) {
			return false;
		}

		if (user.getLockedUntil().isAfter(Instant.now())) {
			return true;
		}

		user.setLockedUntil(null);
		user.setFailedAttempts(0);
		return false;
	}

	private void registerFailedAttempt(PlatformUserEntity user) {
		int attempts = user.getFailedAttempts() + 1;

		if (attempts >= platformProperties.maxFailedAttempts()) {
			user.setFailedAttempts(0);
			user.setLockedUntil(Instant.now().plus(platformProperties.lockDuration()));
			return;
		}

		user.setFailedAttempts(attempts);
	}

	public record LoginResult(PlatformPrincipal principal, String sessionToken) {
	}
}
