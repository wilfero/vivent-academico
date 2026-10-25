package com.corpoviva.vivent.combos.dto;

import com.corpoviva.vivent.combos.entity.ComboOfferItemEntity;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.UUID;

public record ComboOfferItemResponse(
	UUID id,
	UUID productId,
	String productName,
	UUID presentationId,
	String presentationName,
	BigDecimal quantityPresentation,
	BigDecimal conversionFactor,
	BigDecimal quantityBase,
	BigDecimal regularUnitPriceSnapshot
) {

	public static ComboOfferItemResponse from(ComboOfferItemEntity item) {
		if (item.getProduct() == null) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo item " + item.getId() + " has no product relation");
		}
		if (item.getPresentation() == null) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo item " + item.getId() + " has no presentation relation");
		}
		return new ComboOfferItemResponse(
			item.getId(),
			item.getProduct().getId(),
			item.getProduct().getName(),
			item.getPresentation().getId(),
			item.getPresentation().getName(),
			item.getQuantityPresentation(),
			item.getConversionFactor(),
			item.getQuantityBase(),
			item.getRegularUnitPriceSnapshot()
		);
	}
}