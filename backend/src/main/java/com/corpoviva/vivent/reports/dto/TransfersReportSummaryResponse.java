package com.corpoviva.vivent.reports.dto;

import java.math.BigDecimal;

public record TransfersReportSummaryResponse(
	long totalTransfers,
	long requestedTransfers,
	long approvedTransfers,
	long rejectedTransfers,
	long inTransitTransfers,
	long partiallyReceivedTransfers,
	long receivedTransfers,
	long cancelledTransfers,
	long pendingTransfers,
	BigDecimal requestedBaseTotal,
	BigDecimal approvedBaseTotal,
	BigDecimal receivedBaseTotal
) {}
