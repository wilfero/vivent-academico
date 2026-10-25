package com.corpoviva.vivent.reports.dto;

import java.math.BigDecimal;

public record CashClosuresReportSummaryResponse(
	long totalClosures,
	long openClosures,
	long closedClosures,
	long approvedClosures,
	long reopenedClosures,
	BigDecimal expectedTotal,
	BigDecimal countedTotal,
	BigDecimal differenceTotal
) {}
