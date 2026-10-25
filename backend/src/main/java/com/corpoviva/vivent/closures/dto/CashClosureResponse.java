package com.corpoviva.vivent.closures.dto;

import com.corpoviva.vivent.closures.entity.CashClosureEntity;
import com.corpoviva.vivent.closures.entity.CashClosureStatus;
import com.corpoviva.vivent.closures.entity.ClosureType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record CashClosureResponse(
	UUID id,
	UUID branchId,
	String branchName,
	UUID userId,
	String username,
	ClosureType closureType,
	LocalDate closureDate,
	CashClosureStatus status,
	BigDecimal confirmedSalesTotal,
	BigDecimal cancelledSalesTotal,
	BigDecimal discountTotal,
	BigDecimal expectedTotal,
	BigDecimal countedTotal,
	BigDecimal differenceAmount,
	Map<String, BigDecimal> paymentsByMethod,
	boolean outdated,
	long salesAfterClosureCount,
	String notes,
	String timezoneSnapshot,
	Instant createdAt,
	Instant closedAt,
	Instant approvedAt
) {

	public static CashClosureResponse from(CashClosureEntity closure) {
		return from(closure, Map.of(), false, 0L);
	}

	public static CashClosureResponse from(CashClosureEntity closure, Map<String, BigDecimal> paymentsByMethod, boolean outdated, long salesAfterClosureCount) {
		return new CashClosureResponse(
			closure.getId(),
			closure.getBranch().getId(),
			closure.getBranch().getName(),
			closure.getUser().getId(),
			displayName(closure),
			ClosureType.DAILY,
			closure.getClosureDate(),
			closure.getStatus(),
			closure.getConfirmedSalesTotal(),
			closure.getCancelledSalesTotal(),
			closure.getDiscountTotal(),
			closure.getExpectedTotal(),
			closure.getCountedTotal(),
			closure.getDifferenceAmount(),
			paymentsByMethod,
			outdated,
			salesAfterClosureCount,
			closure.getNotes(),
			closure.getTimezoneSnapshot(),
			closure.getCreatedAt(),
			closure.getClosedAt(),
			closure.getApprovedAt()
		);
	}

	private static String displayName(CashClosureEntity closure) {
		String name = (closure.getUser().getFirstName() + " " + closure.getUser().getLastName()).trim();
		return name.isBlank() ? closure.getUser().getUsername() : name;
	}
}
