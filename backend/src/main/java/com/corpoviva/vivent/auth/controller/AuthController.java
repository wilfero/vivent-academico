package com.corpoviva.vivent.auth.controller;

import com.corpoviva.vivent.audit.entity.AuditAction;
import com.corpoviva.vivent.audit.service.AuditLogService;
import com.corpoviva.vivent.auth.dto.CurrentSessionResponse;
import com.corpoviva.vivent.auth.dto.LoginRequest;
import com.corpoviva.vivent.auth.dto.LoginResponse;
import com.corpoviva.vivent.auth.dto.LogoutRequest;
import com.corpoviva.vivent.auth.dto.RefreshTokenRequest;
import com.corpoviva.vivent.auth.service.AuthService;
import com.corpoviva.vivent.auth.service.RefreshTokenService;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final RefreshTokenService refreshTokenService;
	private final AuditLogService auditLogService;

	public AuthController(
		AuthService authService,
		RefreshTokenService refreshTokenService,
		AuditLogService auditLogService
	) {
		this.authService = authService;
		this.refreshTokenService = refreshTokenService;
		this.auditLogService = auditLogService;
	}

	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(
		@Valid @RequestBody LoginRequest request,
		HttpServletRequest httpRequest
	) {
		return ApiResponse.success(authService.login(request, clientIp(httpRequest), userAgent(httpRequest)));
	}

	@PostMapping("/refresh")
	public ApiResponse<LoginResponse> refresh(
		@Valid @RequestBody RefreshTokenRequest request,
		HttpServletRequest httpRequest
	) {
		// Rotation happens inside consume(): the presented token is revoked and
		// a brand-new access + refresh pair is issued.
		var user = refreshTokenService.consume(request.refreshToken());
		return ApiResponse.success(authService.renewSession(user, clientIp(httpRequest), userAgent(httpRequest)));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(
		@Valid @RequestBody LogoutRequest request,
		HttpServletRequest httpRequest
	) {
		// consume() validates + rotates (revokes) the presented refresh token and
		// returns its user, which is used for the LOGOUT audit record.
		var user = refreshTokenService.consume(request.refreshToken());
		logLogout(user);
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	@GetMapping("/me")
	public ApiResponse<CurrentSessionResponse> me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return ApiResponse.success(CurrentSessionResponse.from(authenticatedUser));
	}

	private void logLogout(com.corpoviva.vivent.users.entity.UserEntity user) {
		try {
			AuthenticatedUser auditUser = new AuthenticatedUser(
				user.getCompany().getId(),
				user.getBranch().getId(),
				user.getBranch().getName(),
				user.getId(),
				user.getCompany().getTenantSlug(),
				user.getUsername(),
				user.getRole().getCode(),
				java.util.Set.of(),
				user.isMustChangePassword(),
				user.getAuthVersion(),
				Instant.now()
			);
			auditLogService.log(auditUser, AuditAction.LOGOUT, "user", user.getId(), null, null);
		} catch (RuntimeException ignored) {
			// Never fail logout because of audit resolution issues.
		}
	}

	private String clientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private String userAgent(HttpServletRequest request) {
		return request.getHeader("User-Agent");
	}
}
