package com.corpoviva.vivent.auth.service;

import com.corpoviva.vivent.audit.entity.AuditAction;
import com.corpoviva.vivent.audit.service.AuditLogService;
import com.corpoviva.vivent.auth.dto.LoginRequest;
import com.corpoviva.vivent.auth.dto.LoginResponse;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.entity.CompanyStatus;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.permissions.service.PermissionResolutionService;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.security.JwtService;
import com.corpoviva.vivent.subscriptions.entity.SubscriptionEntity;
import com.corpoviva.vivent.subscriptions.entity.SubscriptionStatus;
import com.corpoviva.vivent.subscriptions.repository.SubscriptionRepository;
import com.corpoviva.vivent.users.entity.UserEntity;
import com.corpoviva.vivent.users.entity.UserStatus;
import com.corpoviva.vivent.users.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final CompanyRepository companyRepository;
	private final UserRepository userRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final PermissionResolutionService permissionResolutionService;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;
	private final AuditLogService auditLogService;

	public AuthService(
		CompanyRepository companyRepository,
		UserRepository userRepository,
		SubscriptionRepository subscriptionRepository,
		PermissionResolutionService permissionResolutionService,
		PasswordEncoder passwordEncoder,
		JwtService jwtService,
		RefreshTokenService refreshTokenService,
		AuditLogService auditLogService
	) {
		this.companyRepository = companyRepository;
		this.userRepository = userRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.permissionResolutionService = permissionResolutionService;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.refreshTokenService = refreshTokenService;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
		CompanyEntity company = loadTenant(request.tenantSlug());
		validateCompany(company);
		validateSubscription(company.getId());

		UserEntity user = userRepository.findForAuthentication(request.tenantSlug(), request.username())
			.orElseThrow(() -> ApiException.unauthorized(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials"));

		validateUserAccess(user);

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw ApiException.unauthorized(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
		}

		user.setLastLoginAt(Instant.now());

		AuthenticatedUser authenticatedUser = buildAuthenticatedUser(user, user.getCompany().getTenantSlug(), null);
		JwtService.IssuedToken issuedToken = jwtService.issueToken(authenticatedUser);
		AuthenticatedUser authenticatedUserWithExpiry = buildAuthenticatedUser(
			user,
			user.getCompany().getTenantSlug(),
			issuedToken.expiresAt()
		);
		RefreshTokenService.IssuedRefreshToken issuedRefreshToken = refreshTokenService.issue(user, ipAddress, userAgent);

		auditLogService.log(authenticatedUserWithExpiry, AuditAction.LOGIN, "user", user.getId(), null, null);

		return LoginResponse.from(
			issuedToken.token(),
			issuedToken.expiresAt(),
			issuedRefreshToken.refreshToken(),
			issuedRefreshToken.expiresAt(),
			authenticatedUserWithExpiry
		);
	}

	@Transactional(readOnly = true)
	public LoginResponse renewSession(UserEntity user, String ipAddress, String userAgent) {
		validateCompany(user.getCompany());
		validateSubscription(user.getCompany().getId());
		validateUserAccess(user);

		AuthenticatedUser authenticatedUser = buildAuthenticatedUser(user, user.getCompany().getTenantSlug(), null);
		JwtService.IssuedToken issuedToken = jwtService.issueToken(authenticatedUser);
		AuthenticatedUser authenticatedUserWithExpiry = buildAuthenticatedUser(
			user,
			user.getCompany().getTenantSlug(),
			issuedToken.expiresAt()
		);
		RefreshTokenService.IssuedRefreshToken issuedRefreshToken = refreshTokenService.issue(user, ipAddress, userAgent);

		return LoginResponse.from(
			issuedToken.token(),
			issuedToken.expiresAt(),
			issuedRefreshToken.refreshToken(),
			issuedRefreshToken.expiresAt(),
			authenticatedUserWithExpiry
		);
	}

	@Transactional(readOnly = true)
	public AuthenticatedUser authenticateToken(String token) {
		JwtService.ParsedToken parsedToken;

		try {
			parsedToken = jwtService.parse(token);
		} catch (JwtException | IllegalArgumentException exception) {
			throw ApiException.unauthorized(ErrorCode.INVALID_TOKEN, "Invalid or expired token");
		}

		UserEntity user = userRepository.findForTokenAuthentication(parsedToken.userId(), parsedToken.companyId())
			.orElseThrow(() -> ApiException.unauthorized(ErrorCode.INVALID_TOKEN, "Invalid token subject"));

		validateCompany(user.getCompany());
		validateSubscription(user.getCompany().getId());
		validateUserAccess(user);
		validateTokenConsistency(parsedToken, user);

		return buildAuthenticatedUser(user, parsedToken.tenantSlug(), parsedToken.expiresAt());
	}

	private CompanyEntity loadTenant(String tenantSlug) {
		return companyRepository.findByTenantSlugAndDeletedAtIsNull(tenantSlug)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Tenant not found"));
	}

	private void validateCompany(CompanyEntity company) {
		if (company.getDeletedAt() != null || company.getStatus() != CompanyStatus.ACTIVE) {
			throw ApiException.forbidden(ErrorCode.COMPANY_INACTIVE, "Company is not active");
		}
	}

	private void validateSubscription(UUID companyId) {
		SubscriptionEntity subscription = subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(companyId)
			.orElseThrow(() -> ApiException.forbidden(ErrorCode.SUBSCRIPTION_EXPIRED, "Subscription is not available"));

		Instant now = Instant.now();

		boolean allowedStatus = subscription.getStatus() == SubscriptionStatus.TRIAL
			|| subscription.getStatus() == SubscriptionStatus.ACTIVE;

		boolean expiredTrial = subscription.getStatus() == SubscriptionStatus.TRIAL
			&& subscription.getTrialEndsAt() != null
			&& subscription.getTrialEndsAt().isBefore(now);

		boolean expiredActive = subscription.getStatus() == SubscriptionStatus.ACTIVE
			&& subscription.getEndsAt() != null
			&& subscription.getEndsAt().isBefore(now);

		if (!allowedStatus || expiredTrial || expiredActive) {
			throw ApiException.forbidden(ErrorCode.SUBSCRIPTION_EXPIRED, "Subscription is expired or blocked");
		}
	}

	private void validateUserAccess(UserEntity user) {
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw ApiException.forbidden(ErrorCode.USER_INACTIVE, "User is not active");
		}

		if (!user.getBranch().isActive() || user.getBranch().getDeletedAt() != null) {
			throw ApiException.forbidden(ErrorCode.USER_INACTIVE, "User branch is not active");
		}

		if (!user.getBranch().getCompany().getId().equals(user.getCompany().getId())) {
			throw ApiException.forbidden(ErrorCode.USER_INACTIVE, "User branch does not belong to the company");
		}
	}

	private void validateTokenConsistency(JwtService.ParsedToken parsedToken, UserEntity user) {
		if (!parsedToken.tenantSlug().equals(user.getCompany().getTenantSlug())) {
			throw ApiException.unauthorized(ErrorCode.INVALID_TOKEN, "Invalid tenant token claim");
		}

		if (!parsedToken.branchId().equals(user.getBranch().getId())) {
			throw ApiException.unauthorized(ErrorCode.INVALID_TOKEN, "Invalid branch token claim");
		}

		if (parsedToken.authVersion() != user.getAuthVersion()) {
			throw ApiException.unauthorized(ErrorCode.INVALID_TOKEN, "Token was revoked");
		}
	}

	private AuthenticatedUser buildAuthenticatedUser(UserEntity user, String tenantSlug, Instant tokenExpiresAt) {
		Set<String> permissions = permissionResolutionService.resolvePermissions(user);

		return new AuthenticatedUser(
			user.getCompany().getId(),
			user.getBranch().getId(),
			user.getBranch().getName(),
			user.getId(),
			tenantSlug,
			user.getUsername(),
			user.getRole().getCode(),
			permissions,
			user.isMustChangePassword(),
			user.getAuthVersion(),
			tokenExpiresAt
		);
	}
}
