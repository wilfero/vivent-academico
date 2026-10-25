package com.corpoviva.vivent.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetUserPasswordRequest(
	@NotBlank @Size(min = 8, max = 120) String password
) {
}
