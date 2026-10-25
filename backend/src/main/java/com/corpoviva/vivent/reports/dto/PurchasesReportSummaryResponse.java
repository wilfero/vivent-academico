package com.corpoviva.vivent.reports.dto;

import java.math.BigDecimal;

public record PurchasesReportSummaryResponse(
	long totalPurchases,
	long confirmedPurchases,
	long voidedPurchases,
	BigDecimal confirmedTotal,
	BigDecimal voidedTotal,
	BigDecimal taxTotal
) {
}
