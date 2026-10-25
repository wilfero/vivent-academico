package com.corpoviva.vivent.users.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateUserRequest(
	@NotBlank @Size(max = 80) String username,
	@Size(min = 8, max = 120) String password,
	@NotBlank @Size(max = 100) String firstName,
	@NotBlank @Size(max = 100) String lastName,
	@NotBlank @Email @Size(max = 150) String email,
	@Size(max = 50) String phone,
	LocalDate birthDate,
	@NotNull UUID roleId,
	@Valid List<PermissionOverrideRequest> permissionOverrides
) {
}
