package com.corpoviva.vivent.inventory.dto;

import com.corpoviva.vivent.inventory.entity.StockMovementEntity;
import com.corpoviva.vivent.inventory.entity.StockMovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record StockMovementResponse(
	UUID id,
	UUID branchId,
	String branchName,
	UUID locationId,
	String locationCode,
	UUID productId,
	String productName,
	String baseUnitCode,
	StockMovementType movementType,
	String sourceType,
	UUID sourceId,
	BigDecimal quantityIn,
	BigDecimal quantityOut,
	BigDecimal unitCost,
	BigDecimal totalCost,
	BigDecimal balanceAfter,
	String notes,
	UUID createdBy,
	String createdByUsername,
	String timezoneSnapshot,
	Instant createdAt
) {

	public static StockMovementResponse from(StockMovementEntity movement) {
		return from(movement, Map.of());
	}

	public static StockMovementResponse from(StockMovementEntity movement, Map<UUID, String> usernames) {
		return new StockMovementResponse(
			movement.getId(),
			movement.getBranch().getId(),
			movement.getBranch().getName(),
			movement.getLocation().getId(),
			movement.getLocation().getCode(),
			movement.getProduct().getId(),
			movement.getProduct().getName(),
			movement.getProduct().getBaseUnitCode(),
			movement.getMovementType(),
			movement.getSourceType(),
			movement.getSourceId(),
			movement.getQuantityIn(),
			movement.getQuantityOut(),
			movement.getUnitCost(),
			movement.getTotalCost(),
			movement.getBalanceAfter(),
			movement.getNotes(),
			movement.getCreatedBy(),
			usernames.get(movement.getCreatedBy()),
			movement.getTimezoneSnapshot(),
			movement.getCreatedAt()
		);
	}
}
