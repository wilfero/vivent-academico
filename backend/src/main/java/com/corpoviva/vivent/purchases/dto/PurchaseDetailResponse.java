package com.corpoviva.vivent.purchases.dto;

import com.corpoviva.vivent.purchases.entity.PurchaseDetailEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseDetailResponse(
	UUID id,
	UUID productId,
	String productName,
	UUID presentationId,
	String presentationName,
	BigDecimal quantityPresentation,
	BigDecimal conversionFactor,
	BigDecimal quantityBase,
	BigDecimal unitCostPresentation,
	BigDecimal unitCostBase,
	BigDecimal subtotal,
	BigDecimal taxAmount,
	BigDecimal totalAmount,
	LocalDate expirationDate,
	String lotNumber
) {

	public static PurchaseDetailResponse from(PurchaseDetailEntity detail) {
		return new PurchaseDetailResponse(
			detail.getId(),
			detail.getProduct().getId(),
			detail.getProduct().getName(),
			detail.getPresentation().getId(),
			detail.getPresentation().getName(),
			detail.getQuantityPresentation(),
			detail.getConversionFactor(),
			detail.getQuantityBase(),
			detail.getUnitCostPresentation(),
			detail.getUnitCostBase(),
			detail.getSubtotal(),
			detail.getTaxAmount(),
			detail.getTotalAmount(),
			detail.getExpirationDate(),
			detail.getLotNumber()
		);
	}
}
