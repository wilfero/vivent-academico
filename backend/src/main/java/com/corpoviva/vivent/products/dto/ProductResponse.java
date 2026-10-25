package com.corpoviva.vivent.products.dto;

import com.corpoviva.vivent.products.entity.ProductEntity;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
	UUID id,
	UUID companyId,
	CategorySummary category,
	String name,
	String description,
	String baseUnitCode,
	String sku,
	BigDecimal minimumStock,
	boolean stockable,
	boolean hasExpirationDate,
	boolean hasLotTracking,
	boolean hasSerialTracking,
	boolean active
) {

	public static ProductResponse from(ProductEntity product) {
		CategorySummary category = product.getCategory() == null
			? null
			: new CategorySummary(product.getCategory().getId(), product.getCategory().getName());

		return new ProductResponse(
			product.getId(),
			product.getCompany().getId(),
			category,
			product.getName(),
			product.getDescription(),
			product.getBaseUnitCode(),
			product.getSku(),
			product.getMinimumStock(),
			product.isStockable(),
			product.isHasExpirationDate(),
			product.isHasLotTracking(),
			product.isHasSerialTracking(),
			product.isActive()
		);
	}

	public record CategorySummary(UUID id, String name) {
	}
}
