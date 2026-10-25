package com.corpoviva.vivent.permissions.dto;

import com.corpoviva.vivent.permissions.entity.PermissionEntity;
import java.util.UUID;

public record PermissionResponse(
	UUID id,
	String code,
	String translationKey,
	String moduleCode,
	String actionCode,
	String description,
	boolean system
) {

	public static PermissionResponse from(PermissionEntity permission) {
		return new PermissionResponse(
			permission.getId(),
			permission.getCode(),
			permission.getTranslationKey(),
			permission.getModuleCode(),
			permission.getActionCode(),
			permission.getDescription(),
			permission.isSystem()
		);
	}
}
