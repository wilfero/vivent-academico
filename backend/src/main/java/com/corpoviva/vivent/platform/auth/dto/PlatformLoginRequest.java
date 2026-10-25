package com.corpoviva.vivent.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlatformLoginRequest(
	@NotBlank @Size(max = 80) String username,
	@NotBlank @Size(max = 120) String password
) {
}
