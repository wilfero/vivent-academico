package com.corpoviva.vivent.users.service;

import com.corpoviva.vivent.audit.entity.AuditAction;
import com.corpoviva.vivent.audit.service.AuditLogService;
import com.corpoviva.vivent.auth.dto.LoginResponse;
import com.corpoviva.vivent.auth.service.AuthService;
import com.corpoviva.vivent.auth.service.RefreshTokenService;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.users.dto.ChangeOwnPasswordRequest;
import com.corpoviva.vivent.users.dto.UpdateOwnProfileRequest;
import com.corpoviva.vivent.users.dto.UserProfileResponse;
import com.corpoviva.vivent.users.entity.UserEntity;
import com.corpoviva.vivent.users.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthService authService;
	private final RefreshTokenService refreshTokenService;
	private final AuditLogService auditLogService;

	public UserProfileService(
		UserRepository userRepository,
		PasswordEncoder passwordEncoder,
		AuthService authService,
		RefreshTokenService refreshTokenService,
		AuditLogService auditLogService
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authService = authService;
		this.refreshTokenService = refreshTokenService;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public UserProfileResponse getProfile(AuthenticatedUser authenticatedUser) {
		return UserProfileResponse.from(loadCurrentUser(authenticatedUser));
	}

	@Transactional
	public UserProfileResponse updateProfile(AuthenticatedUser authenticatedUser, UpdateOwnProfileRequest request) {
		UserEntity user = loadCurrentUser(authenticatedUser);
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
		user.setPhone(trimToNull(request.phone()));
		user.setBirthDate(request.birthDate());

		auditLogService.log(
			authenticatedUser,
			AuditAction.UPDATE,
			"user_profile",
			user.getId(),
			null,
			"{\"selfService\":true,\"fields\":[\"firstName\",\"lastName\",\"email\",\"phone\",\"birthDate\"]}"
		);

		return UserProfileResponse.from(user);
	}

	@Transactional
	public LoginResponse changePassword(
		AuthenticatedUser authenticatedUser,
		ChangeOwnPasswordRequest request,
		String ipAddress,
		String userAgent
	) {
		UserEntity user = loadCurrentUser(authenticatedUser);

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw ApiException.badRequest(ErrorCode.INVALID_CURRENT_PASSWORD, "Current password is invalid");
		}

		if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "New password must be different from current password");
		}

		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		user.setMustChangePassword(false);
		user.setAuthVersion(user.getAuthVersion() + 1);

		// Password changes invalidate every live refresh token; the response
		// carries a brand-new access + refresh pair.
		refreshTokenService.revokeAllForUser(user.getId());

		auditLogService.log(
			authenticatedUser,
			AuditAction.UPDATE,
			"user_password",
			user.getId(),
			null,
			"{\"selfService\":true,\"authVersion\":" + user.getAuthVersion() + "}"
		);

		return authService.renewSession(user, ipAddress, userAgent);
	}

	private UserEntity loadCurrentUser(AuthenticatedUser authenticatedUser) {
		return userRepository.findForTokenAuthentication(authenticatedUser.userId(), authenticatedUser.companyId())
			.orElseThrow(() -> ApiException.unauthorized(ErrorCode.INVALID_TOKEN, "Invalid token subject"));
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
