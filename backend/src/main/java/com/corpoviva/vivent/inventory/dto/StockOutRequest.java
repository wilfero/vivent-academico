package com.corpoviva.vivent.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record StockOutRequest(
	@NotNull UUID branchId,
	UUID locationId,
	@NotNull UUID productId,
	@NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
	@Size(max = 1000) String notes
) {
}
