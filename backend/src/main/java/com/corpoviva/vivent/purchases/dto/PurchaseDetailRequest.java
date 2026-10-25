package com.corpoviva.vivent.purchases.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseDetailRequest(
	@NotNull UUID presentationId,
	@NotNull @DecimalMin(value = "0.000001") BigDecimal quantityPresentation,
	@NotNull @DecimalMin(value = "0.00") BigDecimal unitCostPresentation,
	@DecimalMin(value = "0.00") BigDecimal taxAmount,
	LocalDate expirationDate,
	String lotNumber
) {}
