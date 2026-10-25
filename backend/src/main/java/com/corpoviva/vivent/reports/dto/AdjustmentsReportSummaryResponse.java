package com.corpoviva.vivent.reports.dto;

import java.math.BigDecimal;

public record AdjustmentsReportSummaryResponse(
	long totalAdjustments,
	long draftAdjustments,
	long pendingApprovalAdjustments,
	long approvedAdjustments,
	long rejectedAdjustments,
	long cancelledAdjustments,
	BigDecimal quantityInBase,
	BigDecimal quantityOutBase
) {}
