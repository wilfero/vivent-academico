package com.corpoviva.vivent.closures.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record CashClosurePreviewResponse(
	UUID branchId,
	String branchName,
	LocalDate closureDate,
	BigDecimal confirmedSalesTotal,
	BigDecimal cancelledSalesTotal,
	BigDecimal discountTotal,
	BigDecimal expectedTotal,
	Map<String, BigDecimal> paymentsByMethod
) {
}
