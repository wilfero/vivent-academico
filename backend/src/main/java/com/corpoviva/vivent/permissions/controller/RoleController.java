package com.corpoviva.vivent.permissions.controller;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.permissions.dto.RoleResponse;
import com.corpoviva.vivent.permissions.dto.UpsertRoleRequest;
import com.corpoviva.vivent.permissions.service.RoleService;
import com.corpoviva.vivent.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/v1/roles")
public class RoleController {

	private final RoleService roleService;

	public RoleController(RoleService roleService) {
		this.roleService = roleService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('settings.view')")
	public ApiResponse<PageResponse<RoleResponse>> listRoles(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) String query
	) {
		return ApiResponse.success(roleService.listRoles(authenticatedUser, page, pageSize, query));
	}

	@GetMapping("/{roleId}")
	@PreAuthorize("hasAuthority('settings.view')")
	public ApiResponse<RoleResponse> getRole(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable UUID roleId
	) {
		return ApiResponse.success(roleService.getRole(authenticatedUser, roleId));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('settings.update')")
	public ApiResponse<RoleResponse> createRole(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody UpsertRoleRequest request
	) {
		return ApiResponse.success(roleService.createRole(authenticatedUser, request));
	}

	@PutMapping("/{roleId}")
	@PreAuthorize("hasAuthority('settings.update')")
	public ApiResponse<RoleResponse> updateRole(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable UUID roleId,
		@Valid @RequestBody UpsertRoleRequest request
	) {
		return ApiResponse.success(roleService.updateRole(authenticatedUser, roleId, request));
	}
}
