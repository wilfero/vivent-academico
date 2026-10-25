package com.corpoviva.vivent.closures.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record CashClosureCloseRequest(
	@DecimalMin("0.00") BigDecimal countedTotal
) {
}
