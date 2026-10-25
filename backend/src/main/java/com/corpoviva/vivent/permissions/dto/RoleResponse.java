package com.corpoviva.vivent.permissions.dto;

import com.corpoviva.vivent.permissions.entity.RoleEntity;
import java.util.Set;
import java.util.UUID;

public record RoleResponse(
	UUID id,
	UUID companyId,
	String code,
	String name,
	String translationKey,
	String description,
	boolean system,
	Set<String> permissionCodes
) {

	public static RoleResponse from(RoleEntity role) {
		return new RoleResponse(
			role.getId(),
			role.getCompanyId(),
			role.getCode(),
			role.getName(),
			role.getTranslationKey(),
			role.getDescription(),
			role.isSystem(),
			role.getPermissions().stream().map(permission -> permission.getCode()).collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new))
		);
	}
}
