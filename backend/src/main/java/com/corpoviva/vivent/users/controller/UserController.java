package com.corpoviva.vivent.users.controller;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.users.dto.ChangeUserBranchRequest;
import com.corpoviva.vivent.users.dto.CreateUserRequest;
import com.corpoviva.vivent.users.dto.ResetUserPasswordRequest;
import com.corpoviva.vivent.users.dto.UpdateUserRequest;
import com.corpoviva.vivent.users.dto.UserResponse;
import com.corpoviva.vivent.users.entity.UserStatus;
import com.corpoviva.vivent.users.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('users.view')")
	public ApiResponse<PageResponse<UserResponse>> listUsers(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) String query,
		@RequestParam(required = false) UserStatus status,
		@RequestParam(required = false) UUID branchId
	) {
		return ApiResponse.success(userService.listUsers(authenticatedUser, page, pageSize, query, status, branchId));
	}

	@GetMapping("/{userId}")
	@PreAuthorize("hasAuthority('users.view')")
	public ApiResponse<UserResponse> getUser(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable UUID userId
	) {
		return ApiResponse.success(userService.getUser(authenticatedUser, userId));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('users.create')")
	public ApiResponse<UserResponse> createUser(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody CreateUserRequest request
	) {
		return ApiResponse.success(userService.createUser(authenticatedUser, request));
	}

	@PutMapping("/{userId}")
	@PreAuthorize("hasAuthority('users.update')")
	public ApiResponse<UserResponse> updateUser(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable UUID userId,
		@Valid @RequestBody UpdateUserRequest request
	) {
		return ApiResponse.success(userService.updateUser(authenticatedUser, userId, request));
	}

	@PostMapping("/{userId}/activate")
	@PreAuthorize("hasAuthority('users.update')")
	public ApiResponse<UserResponse> activateUser(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable UUID userId
	) {
		return ApiResponse.success(userService.activateUser(authenticatedUser, userId));
	}

	@PostMapping("/{userId}/deactivate")
	@PreAuthorize("hasAuthority('users.deactivate')")
	public ApiResponse<UserResponse> deactivateUser(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable UUID userId
	) {
		return ApiResponse.success(userService.deactivateUser(authenticatedUser, userId));
	}

	@PostMapping("/{userId}/branch")
	@PreAuthorize("hasAuthority('users.change_branch')")
	public ApiResponse<UserResponse> changeUserBranch(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable UUID userId,
		@Valid @RequestBody ChangeUserBranchRequest request
	) {
		return ApiResponse.success(userService.changeUserBranch(authenticatedUser, userId, request));
	}

	@PostMapping("/{userId}/password")
	@PreAuthorize("hasAuthority('users.update')")
	public ApiResponse<UserResponse> resetUserPassword(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable UUID userId,
		@Valid @RequestBody ResetUserPasswordRequest request
	) {
		return ApiResponse.success(userService.resetUserPassword(authenticatedUser, userId, request));
	}

	@DeleteMapping("/{userId}")
	@PreAuthorize("hasAuthority('users.delete')")
	public ApiResponse<Void> deleteUser(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable UUID userId
	) {
		userService.deleteUser(authenticatedUser, userId);
		return ApiResponse.success(null);
	}
}
