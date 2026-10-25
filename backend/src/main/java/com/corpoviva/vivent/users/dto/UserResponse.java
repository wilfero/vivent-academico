package com.corpoviva.vivent.users.dto;

import com.corpoviva.vivent.users.entity.UserEntity;
import com.corpoviva.vivent.users.entity.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
	UUID id,
	UUID companyId,
	String username,
	String firstName,
	String lastName,
	String email,
	String phone,
	LocalDate birthDate,
	UserStatus status,
	Instant lastLoginAt,
	BranchReference branch,
	RoleReference role,
	List<PermissionOverrideResponse> permissionOverrides,
	Set<String> resolvedPermissions
) {

	public static UserResponse from(UserEntity user, Set<String> resolvedPermissions) {
		List<PermissionOverrideResponse> overrides = user.getPermissionOverrides().stream()
			.map(override -> new PermissionOverrideResponse(override.getPermission().getCode(), override.getEffect()))
			.toList();

		return new UserResponse(
			user.getId(),
			user.getCompany().getId(),
			user.getUsername(),
			user.getFirstName(),
			user.getLastName(),
			user.getEmail(),
			user.getPhone(),
			user.getBirthDate(),
			user.getStatus(),
			user.getLastLoginAt(),
			new BranchReference(user.getBranch().getId(), user.getBranch().getName(), user.getBranch().getCode(), user.getBranch().isActive()),
			new RoleReference(user.getRole().getId(), user.getRole().getCode(), user.getRole().getName(), user.getRole().isSystem()),
			overrides,
			resolvedPermissions
		);
	}

	public record BranchReference(UUID id, String name, String code, boolean active) {
	}

	public record RoleReference(UUID id, String code, String name, boolean system) {
	}
}
