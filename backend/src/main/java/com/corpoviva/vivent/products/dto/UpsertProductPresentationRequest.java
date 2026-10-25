package com.corpoviva.vivent.products.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpsertProductPresentationRequest(
	@NotBlank @Size(max = 150) String name,
	@Size(max = 60) String barcode,
	@NotNull @DecimalMin(value = "0.000001") BigDecimal conversionFactor,
	@NotNull @DecimalMin("0.00") BigDecimal purchasePriceSuggested,
	@NotNull @DecimalMin("0.00") BigDecimal salePrice,
	boolean defaultPurchase,
	boolean defaultSale,
	boolean active
) {
}
