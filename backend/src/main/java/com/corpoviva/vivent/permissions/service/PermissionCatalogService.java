package com.corpoviva.vivent.permissions.service;

import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.permissions.entity.PermissionEntity;
import com.corpoviva.vivent.permissions.repository.PermissionRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionCatalogService {

	private final PermissionRepository permissionRepository;

	public PermissionCatalogService(PermissionRepository permissionRepository) {
		this.permissionRepository = permissionRepository;
	}

	@Transactional(readOnly = true)
	public List<PermissionEntity> findAll() {
		return permissionRepository.findAllByOrderByModuleCodeAscActionCodeAsc();
	}

	@Transactional(readOnly = true)
	public Set<PermissionEntity> resolvePermissionEntities(Collection<String> permissionCodes) {
		List<PermissionEntity> permissions = permissionRepository.findByCodeIn(permissionCodes);
		Map<String, PermissionEntity> permissionByCode = new LinkedHashMap<>();
		permissions.forEach(permission -> permissionByCode.put(permission.getCode(), permission));

		for (String permissionCode : permissionCodes) {
			if (!permissionByCode.containsKey(permissionCode)) {
				throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Unknown permission code: " + permissionCode);
			}
		}

		return Set.copyOf(permissions);
	}
}
