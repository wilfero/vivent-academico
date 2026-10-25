package com.corpoviva.vivent.inventory.dto;

public record InventorySummaryResponse(
	long productsWithStock,
	long productsWithoutStock,
	Long lowStockProducts,
	long totalMovements
) {
}
