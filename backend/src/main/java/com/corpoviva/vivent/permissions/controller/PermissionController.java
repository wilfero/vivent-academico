package com.corpoviva.vivent.permissions.controller;

import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.permissions.dto.PermissionResponse;
import com.corpoviva.vivent.permissions.service.PermissionService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

	private final PermissionService permissionService;

	public PermissionController(PermissionService permissionService) {
		this.permissionService = permissionService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('settings.view')")
	public ApiResponse<List<PermissionResponse>> listPermissions() {
		return ApiResponse.success(permissionService.listPermissions());
	}
}
