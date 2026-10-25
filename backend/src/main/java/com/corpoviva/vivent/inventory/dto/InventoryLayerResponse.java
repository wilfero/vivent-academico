package com.corpoviva.vivent.inventory.dto;

import com.corpoviva.vivent.inventory.entity.InventoryLayerEntity;
import com.corpoviva.vivent.inventory.entity.InventoryLayerStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryLayerResponse(
	UUID id,
	UUID branchId,
	String branchName,
	UUID locationId,
	String locationCode,
	UUID productId,
	String productName,
	String baseUnitCode,
	String sourceType,
	UUID sourceId,
	BigDecimal quantityReceived,
	BigDecimal quantityRemaining,
	BigDecimal unitCost,
	Instant receivedAt,
	LocalDate expirationDate,
	String lotNumber,
	InventoryLayerStatus status,
	String timezoneSnapshot
) {

	public static InventoryLayerResponse from(InventoryLayerEntity layer) {
		return new InventoryLayerResponse(
			layer.getId(),
			layer.getBranch().getId(),
			layer.getBranch().getName(),
			layer.getLocation().getId(),
			layer.getLocation().getCode(),
			layer.getProduct().getId(),
			layer.getProduct().getName(),
			layer.getProduct().getBaseUnitCode(),
			layer.getSourceType(),
			layer.getSourceId(),
			layer.getQuantityReceived(),
			layer.getQuantityRemaining(),
			layer.getUnitCost(),
			layer.getReceivedAt(),
			layer.getExpirationDate(),
			layer.getLotNumber(),
			layer.getStatus(),
			layer.getTimezoneSnapshot()
		);
	}
}
