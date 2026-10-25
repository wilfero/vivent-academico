package com.corpoviva.vivent.users.controller;

import com.corpoviva.vivent.auth.dto.LoginResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.users.dto.ChangeOwnPasswordRequest;
import com.corpoviva.vivent.users.dto.UpdateOwnProfileRequest;
import com.corpoviva.vivent.users.dto.UserProfileResponse;
import com.corpoviva.vivent.users.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserProfileController {

	private final UserProfileService userProfileService;

	public UserProfileController(UserProfileService userProfileService) {
		this.userProfileService = userProfileService;
	}

	@GetMapping
	public ApiResponse<UserProfileResponse> getProfile(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return ApiResponse.success(userProfileService.getProfile(authenticatedUser));
	}

	@PutMapping
	public ApiResponse<UserProfileResponse> updateProfile(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody UpdateOwnProfileRequest request
	) {
		return ApiResponse.success(userProfileService.updateProfile(authenticatedUser, request));
	}

	@PostMapping("/password")
	public ApiResponse<LoginResponse> changePassword(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody ChangeOwnPasswordRequest request,
		jakarta.servlet.http.HttpServletRequest httpRequest
	) {
		return ApiResponse.success(userProfileService.changePassword(
			authenticatedUser,
			request,
			com.corpoviva.vivent.common.util.RequestMetadata.clientIp(httpRequest),
			com.corpoviva.vivent.common.util.RequestMetadata.userAgent(httpRequest)
		));
	}
}
