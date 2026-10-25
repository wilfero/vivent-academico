package com.corpoviva.vivent.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpsertProductUnitRequest(
	@NotBlank @Pattern(regexp = "^[a-z][a-z0-9_-]*$") @Size(max = 30) String code,
	@NotBlank @Size(max = 150) String name,
	@NotBlank @Size(max = 150) String translationKey,
	boolean allowsDecimal,
	boolean active
) {
}
