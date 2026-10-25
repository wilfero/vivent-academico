package com.corpoviva.vivent.permissions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UpsertRoleRequest(
	@NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]*$") @Size(max = 100) String code,
	@NotBlank @Size(max = 150) String name,
	@Size(max = 500) String description,
	@NotEmpty Set<@NotBlank String> permissionCodes
) {
}
