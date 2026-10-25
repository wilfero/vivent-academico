package com.corpoviva.vivent.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StockInRequest(
	@NotNull UUID branchId,
	UUID locationId,
	@NotNull UUID productId,
	@NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
	@NotNull @DecimalMin("0.00") BigDecimal unitCost,
	LocalDate expirationDate,
	@Size(max = 80) String lotNumber,
	@Size(max = 1000) String notes
) {
}
