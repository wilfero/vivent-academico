package com.corpoviva.vivent.products.dto;

import com.corpoviva.vivent.products.entity.ProductPresentationEntity;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductPresentationResponse(
	UUID id,
	UUID companyId,
	UUID productId,
	String name,
	String barcode,
	BigDecimal conversionFactor,
	BigDecimal purchasePriceSuggested,
	BigDecimal salePrice,
	boolean defaultPurchase,
	boolean defaultSale,
	boolean active
) {

	public static ProductPresentationResponse from(ProductPresentationEntity presentation) {
		return new ProductPresentationResponse(
			presentation.getId(),
			presentation.getCompany().getId(),
			presentation.getProduct().getId(),
			presentation.getName(),
			presentation.getBarcode(),
			presentation.getConversionFactor(),
			presentation.getPurchasePriceSuggested(),
			presentation.getSalePrice(),
			presentation.isDefaultPurchase(),
			presentation.isDefaultSale(),
			presentation.isActive()
		);
	}
}
