package com.corpoviva.vivent.closures.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CashClosureRequest(
	@NotNull UUID branchId,
	@NotNull LocalDate closureDate,
	@DecimalMin("0.00") BigDecimal countedTotal,
	String notes
) {
}
