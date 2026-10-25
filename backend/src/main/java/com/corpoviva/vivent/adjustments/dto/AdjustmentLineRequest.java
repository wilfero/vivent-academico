package com.corpoviva.vivent.adjustments.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.UUID;

public record AdjustmentLineRequest(
	UUID productId,
	UUID presentationId,
	@DecimalMin(value = "0.000001") BigDecimal quantityPresentation,
	@DecimalMin(value = "0.000001") BigDecimal quantityBase,
	@DecimalMin(value = "0.00") BigDecimal unitCost,
	String notes
) {}
