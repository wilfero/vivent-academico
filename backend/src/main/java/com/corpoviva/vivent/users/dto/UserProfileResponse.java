package com.corpoviva.vivent.users.dto;

import com.corpoviva.vivent.users.entity.UserEntity;
import java.time.LocalDate;
import java.util.UUID;

public record UserProfileResponse(
	UUID id,
	String username,
	String firstName,
	String lastName,
	String email,
	String phone,
	LocalDate birthDate,
	BranchReference branch,
	RoleReference role,
	boolean mustChangePassword
) {

	public static UserProfileResponse from(UserEntity user) {
		return new UserProfileResponse(
			user.getId(),
			user.getUsername(),
			user.getFirstName(),
			user.getLastName(),
			user.getEmail(),
			user.getPhone(),
			user.getBirthDate(),
			new BranchReference(user.getBranch().getId(), user.getBranch().getName(), user.getBranch().getCode()),
			new RoleReference(user.getRole().getId(), user.getRole().getCode(), user.getRole().getName()),
			user.isMustChangePassword()
		);
	}

	public record BranchReference(UUID id, String name, String code) {
	}

	public record RoleReference(UUID id, String code, String name) {
	}
}
