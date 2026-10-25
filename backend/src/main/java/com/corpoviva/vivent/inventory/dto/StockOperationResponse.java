package com.corpoviva.vivent.inventory.dto;

import java.util.List;
import java.util.UUID;

public record StockOperationResponse(
	UUID operationId,
	InventoryBalanceResponse balance,
	StockMovementResponse movement,
	List<InventoryLayerResponse> affectedLayers
) {
}
