package com.corpoviva.vivent.sales.dto;

import com.corpoviva.vivent.sales.entity.SaleDetailEntity;
import com.corpoviva.vivent.sales.entity.SaleDetailSourceType;
import java.math.BigDecimal;
import java.util.UUID;

public record SaleDetailResponse(
	UUID id,
	UUID productId,
	String productName,
	UUID presentationId,
	String presentationName,
	String barcode,
	BigDecimal quantityPresentation,
	BigDecimal conversionFactor,
	BigDecimal quantityBase,
	BigDecimal unitPrice,
	BigDecimal discountAmount,
	BigDecimal taxAmount,
	BigDecimal totalAmount,
	BigDecimal fifoCostTotal,
	SaleDetailSourceType sourceType,
	UUID comboLineId
) {

	public static SaleDetailResponse from(SaleDetailEntity detail) {
		return new SaleDetailResponse(
			detail.getId(),
			detail.getProduct().getId(),
			detail.getProduct().getName(),
			detail.getPresentation().getId(),
			detail.getPresentation().getName(),
			detail.getBarcode(),
			detail.getQuantityPresentation(),
			detail.getConversionFactor(),
			detail.getQuantityBase(),
			detail.getUnitPrice(),
			detail.getDiscountAmount(),
			detail.getTaxAmount(),
			detail.getTotalAmount(),
			detail.getFifoCostTotal(),
			detail.getSourceType(),
			detail.getComboLineId()
		);
	}
}
