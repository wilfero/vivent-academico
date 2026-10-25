package com.corpoviva.vivent.transfers.dto;

import com.corpoviva.vivent.transfers.entity.TransferDetailEntity;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferDetailResponse(
	UUID id,
	UUID productId,
	String productName,
	UUID presentationId,
	String presentationName,
	BigDecimal quantityRequestedPresentation,
	BigDecimal conversionFactor,
	BigDecimal quantityRequestedBase,
	BigDecimal quantityApprovedBase,
	BigDecimal quantityReceivedBase
) {

	public static TransferDetailResponse from(TransferDetailEntity detail) {
		return new TransferDetailResponse(
			detail.getId(),
			detail.getProduct().getId(),
			detail.getProduct().getName(),
			detail.getPresentation().getId(),
			detail.getPresentation().getName(),
			detail.getQuantityRequestedPresentation(),
			detail.getConversionFactor(),
			detail.getQuantityRequestedBase(),
			detail.getQuantityApprovedBase(),
			detail.getQuantityReceivedBase()
		);
	}
}
