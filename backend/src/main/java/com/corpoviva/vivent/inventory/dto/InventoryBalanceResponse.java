package com.corpoviva.vivent.inventory.dto;

import com.corpoviva.vivent.inventory.entity.InventoryBalanceEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryBalanceResponse(
	UUID id,
	UUID companyId,
	UUID branchId,
	String branchName,
	UUID locationId,
	String locationCode,
	UUID productId,
	String productName,
	String categoryName,
	String baseUnitCode,
	BigDecimal minimumStock,
	BigDecimal quantityOnHand,
	BigDecimal quantityReserved,
	BigDecimal quantityAvailable,
	Instant updatedAt
) {

	public static InventoryBalanceResponse from(InventoryBalanceEntity balance) {
		return new InventoryBalanceResponse(
			balance.getId(),
			balance.getCompany().getId(),
			balance.getBranch().getId(),
			balance.getBranch().getName(),
			balance.getLocation().getId(),
			balance.getLocation().getCode(),
			balance.getProduct().getId(),
			balance.getProduct().getName(),
			balance.getProduct().getCategory() == null ? null : balance.getProduct().getCategory().getName(),
			balance.getProduct().getBaseUnitCode(),
			balance.getProduct().getMinimumStock(),
			balance.getQuantityOnHand(),
			balance.getQuantityReserved(),
			balance.getQuantityOnHand().subtract(balance.getQuantityReserved()),
			balance.getUpdatedAt()
		);
	}
}
