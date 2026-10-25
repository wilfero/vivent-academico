package com.corpoviva.vivent.auth.controller;

import com.corpoviva.vivent.auth.dto.CurrentSessionResponse;
import com.corpoviva.vivent.auth.dto.LoginRequest;
import com.corpoviva.vivent.auth.dto.LoginResponse;
import com.corpoviva.vivent.auth.service.AuthService;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.security.AuthenticatedUser;
import jakarta.validation.Valid;
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

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(authService.login(request));
	}

	@GetMapping("/me")
	public ApiResponse<CurrentSessionResponse> me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return ApiResponse.success(CurrentSessionResponse.from(authenticatedUser));
	}
}
