package com.corpoviva.vivent.users.dto;

import com.corpoviva.vivent.users.entity.PermissionEffect;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PermissionOverrideRequest(
	@NotBlank String permissionCode,
	@NotNull PermissionEffect effect
) {
}
