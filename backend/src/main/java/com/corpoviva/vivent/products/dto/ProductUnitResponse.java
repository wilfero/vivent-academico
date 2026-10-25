package com.corpoviva.vivent.products.dto;

import com.corpoviva.vivent.products.entity.ProductUnitEntity;

public record ProductUnitResponse(
	String code,
	String name,
	String translationKey,
	boolean allowsDecimal,
	boolean active
) {

	public static ProductUnitResponse from(ProductUnitEntity unit) {
		return new ProductUnitResponse(
			unit.getCode(),
			unit.getName(),
			unit.getTranslationKey(),
			unit.isAllowsDecimal(),
			unit.isActive()
		);
	}
}
