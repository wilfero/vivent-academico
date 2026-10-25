package com.corpoviva.vivent.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpsertCategoryRequest(
	UUID parentCategoryId,
	@NotBlank @Size(max = 150) String name,
	@Size(max = 1000) String description,
	boolean active
) {
}
