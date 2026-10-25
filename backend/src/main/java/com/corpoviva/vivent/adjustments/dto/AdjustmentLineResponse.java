package com.corpoviva.vivent.adjustments.dto;

import com.corpoviva.vivent.adjustments.entity.InventoryAdjustmentLineEntity;
import java.math.BigDecimal;
import java.util.UUID;

public record AdjustmentLineResponse(
	UUID id,
	UUID productId,
	String productName,
	UUID presentationId,
	String presentationName,
	BigDecimal quantityPresentation,
	BigDecimal conversionFactor,
	BigDecimal quantityBase,
	BigDecimal unitCost,
	String notes
) {

	public static AdjustmentLineResponse from(InventoryAdjustmentLineEntity line) {
		return new AdjustmentLineResponse(
			line.getId(),
			line.getProduct().getId(),
			line.getProduct().getName(),
			line.getPresentation() == null ? null : line.getPresentation().getId(),
			line.getPresentation() == null ? null : line.getPresentation().getName(),
			line.getQuantityPresentation(),
			line.getConversionFactor(),
			line.getQuantityBase(),
			line.getUnitCost(),
			line.getNotes()
		);
	}
}
