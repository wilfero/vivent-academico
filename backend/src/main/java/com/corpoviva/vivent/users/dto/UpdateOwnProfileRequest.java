package com.corpoviva.vivent.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateOwnProfileRequest(
	@NotBlank @Size(max = 100) String firstName,
	@NotBlank @Size(max = 100) String lastName,
	@NotBlank @Email @Size(max = 150) String email,
	@Size(max = 50) String phone,
	LocalDate birthDate
) {
}
