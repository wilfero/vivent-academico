package com.corpoviva.vivent.reports.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductReportItemResponse(
	UUID productId,
	String productName,
	String categoryName,
	UUID branchId,
	String branchName,
	String baseUnitCode,
	BigDecimal quantity,
	BigDecimal amount,
	long movementCount
) {
}
