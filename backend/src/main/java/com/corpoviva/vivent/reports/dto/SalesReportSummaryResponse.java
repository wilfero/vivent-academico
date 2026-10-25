package com.corpoviva.vivent.reports.dto;

import java.math.BigDecimal;

public record SalesReportSummaryResponse(
	long totalSales,
	long confirmedSales,
	long cancelledSales,
	BigDecimal confirmedTotal,
	BigDecimal cancelledTotal,
	BigDecimal discountTotal
) {
}
