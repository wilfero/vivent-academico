package com.corpoviva.vivent.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record UpsertProductRequest(
	UUID categoryId,
	@NotBlank @Size(max = 150) String name,
	@Size(max = 1000) String description,
	@NotBlank @Size(max = 30) String baseUnitCode,
	@Size(max = 80) String sku,
	@DecimalMin("0.000000") BigDecimal minimumStock,
	boolean stockable,
	boolean hasExpirationDate,
	boolean hasLotTracking,
	boolean hasSerialTracking,
	boolean active
) {
}
