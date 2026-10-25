package com.corpoviva.vivent.platform.auth.controller;

import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.platform.auth.dto.PlatformLoginRequest;
import com.corpoviva.vivent.platform.auth.dto.PlatformSessionResponse;
import com.corpoviva.vivent.platform.auth.service.PlatformAuthService;
import com.corpoviva.vivent.platform.security.PlatformCookieService;
import com.corpoviva.vivent.platform.security.PlatformPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform/auth")
public class PlatformAuthController {

	private final PlatformAuthService platformAuthService;
	private final PlatformCookieService platformCookieService;

	public PlatformAuthController(PlatformAuthService platformAuthService, PlatformCookieService platformCookieService) {
		this.platformAuthService = platformAuthService;
		this.platformCookieService = platformCookieService;
	}

	@PostMapping("/login")
	public ApiResponse<PlatformSessionResponse> login(
		@Valid @RequestBody PlatformLoginRequest request,
		HttpServletRequest httpRequest,
		HttpServletResponse httpResponse
	) {
		PlatformAuthService.LoginResult result = platformAuthService.login(
			request.username(),
			request.password(),
			httpRequest.getRemoteAddr(),
			httpRequest.getHeader("User-Agent")
		);

		platformCookieService.writeSessionCookie(httpResponse, result.sessionToken());
		return ApiResponse.success(PlatformSessionResponse.from(result.principal()));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(
		@AuthenticationPrincipal PlatformPrincipal principal,
		HttpServletRequest httpRequest,
		HttpServletResponse httpResponse
	) {
		String token = platformCookieService.readSessionToken(httpRequest);
		platformAuthService.logout(token, principal == null ? null : principal.userId(), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
		platformCookieService.clearSessionCookie(httpResponse);
		return ApiResponse.success(null);
	}

	@GetMapping("/me")
	public ApiResponse<PlatformSessionResponse> me(@AuthenticationPrincipal PlatformPrincipal principal) {
		return ApiResponse.success(PlatformSessionResponse.from(principal));
	}
}
