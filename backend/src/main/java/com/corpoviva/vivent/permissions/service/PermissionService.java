package com.corpoviva.vivent.permissions.service;

import com.corpoviva.vivent.permissions.dto.PermissionResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionService {

	private final PermissionCatalogService permissionCatalogService;

	public PermissionService(PermissionCatalogService permissionCatalogService) {
		this.permissionCatalogService = permissionCatalogService;
	}

	@Transactional(readOnly = true)
	public List<PermissionResponse> listPermissions() {
		return permissionCatalogService.findAll().stream().map(PermissionResponse::from).toList();
	}
}
