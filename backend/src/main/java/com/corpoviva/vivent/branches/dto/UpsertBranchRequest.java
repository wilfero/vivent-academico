package com.corpoviva.vivent.branches.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpsertBranchRequest(
	@NotBlank @Size(max = 150) String name,
	@NotBlank @Pattern(regexp = "^[A-Z0-9_-]+$") @Size(max = 50) String code,
	@Size(max = 255) String address,
	@Size(max = 50) String phone,
	@Email @Size(max = 150) String email,
	boolean active
) {
}
