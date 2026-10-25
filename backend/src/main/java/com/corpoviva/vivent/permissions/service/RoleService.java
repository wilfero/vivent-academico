package com.corpoviva.vivent.permissions.service;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.permissions.dto.RoleResponse;
import com.corpoviva.vivent.permissions.dto.UpsertRoleRequest;
import com.corpoviva.vivent.permissions.entity.RoleEntity;
import com.corpoviva.vivent.permissions.repository.RoleRepository;
import com.corpoviva.vivent.security.AuthenticatedUser;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

	private final RoleRepository roleRepository;
	private final PermissionCatalogService permissionCatalogService;

	public RoleService(RoleRepository roleRepository, PermissionCatalogService permissionCatalogService) {
		this.roleRepository = roleRepository;
		this.permissionCatalogService = permissionCatalogService;
	}

	@Transactional(readOnly = true)
	public PageResponse<RoleResponse> listRoles(AuthenticatedUser authenticatedUser, int page, int pageSize, String query) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		Page<RoleEntity> rolePage = roleRepository.searchAccessibleRoles(
			authenticatedUser.companyId(),
			normalizeQuery(query),
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("system").descending().and(Sort.by("name").ascending()))
		);

		Page<RoleResponse> responsePage = rolePage.map(RoleResponse::from);
		return PageResponse.from(responsePage, normalizedPage, normalizedPageSize);
	}

	@Transactional(readOnly = true)
	public RoleResponse getRole(AuthenticatedUser authenticatedUser, UUID roleId) {
		return RoleResponse.from(loadAccessibleRole(authenticatedUser.companyId(), roleId));
	}

	@Transactional
	public RoleResponse createRole(AuthenticatedUser authenticatedUser, UpsertRoleRequest request) {
		String normalizedCode = normalizeCode(request.code());
		validateRoleCodeAvailable(authenticatedUser.companyId(), normalizedCode, null);

		RoleEntity role = new RoleEntity();
		role.setCompanyId(authenticatedUser.companyId());
		role.setCode(normalizedCode);
		role.setName(request.name().trim());
		role.setTranslationKey("role.custom." + normalizedCode.toLowerCase(Locale.ROOT));
		role.setDescription(trimToNull(request.description()));
		role.setSystem(false);
		role.getPermissions().clear();
		role.getPermissions().addAll(permissionCatalogService.resolvePermissionEntities(request.permissionCodes()));

		return RoleResponse.from(roleRepository.save(role));
	}

	@Transactional
	public RoleResponse updateRole(AuthenticatedUser authenticatedUser, UUID roleId, UpsertRoleRequest request) {
		RoleEntity role = loadAccessibleRole(authenticatedUser.companyId(), roleId);

		if (role.isSystem()) {
			throw ApiException.forbidden(ErrorCode.PERMISSION_DENIED, "System roles cannot be modified");
		}

		String normalizedCode = normalizeCode(request.code());
		validateRoleCodeAvailable(authenticatedUser.companyId(), normalizedCode, role.getId());

		role.setCode(normalizedCode);
		role.setName(request.name().trim());
		role.setTranslationKey("role.custom." + normalizedCode.toLowerCase(Locale.ROOT));
		role.setDescription(trimToNull(request.description()));
		role.getPermissions().clear();
		role.getPermissions().addAll(permissionCatalogService.resolvePermissionEntities(request.permissionCodes()));

		return RoleResponse.from(role);
	}

	private RoleEntity loadAccessibleRole(UUID companyId, UUID roleId) {
		return roleRepository.findAccessibleById(roleId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Role not found"));
	}

	private void validateRoleCodeAvailable(UUID companyId, String code, UUID excludedRoleId) {
		if (roleRepository.existsAccessibleRoleCode(companyId, code, excludedRoleId)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Role code already exists in the tenant scope");
		}
	}

	private String normalizeCode(String code) {
		return code.trim().toUpperCase(Locale.ROOT);
	}

	private String normalizeQuery(String query) {
		if (query == null) {
			return "";
		}

		String trimmed = query.trim();
		return trimmed.isEmpty() ? "" : trimmed;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
