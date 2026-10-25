package com.corpoviva.vivent.sales.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record SaleDetailRequest(
	UUID presentationId,
	String barcode,
	@NotNull @DecimalMin(value = "0.000001") BigDecimal quantityPresentation,
	@DecimalMin(value = "0.00") BigDecimal unitPrice,
	@DecimalMin(value = "0.00") BigDecimal discountAmount,
	@DecimalMin(value = "0.00") BigDecimal taxAmount
) {}
