package com.corpoviva.vivent.combos.dto;

import com.corpoviva.vivent.combos.entity.ComboOfferEntity;
import com.corpoviva.vivent.combos.entity.ComboOfferItemEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ComboOfferResponse(
	UUID id,
	String name,
	String code,
	String barcode,
	String description,
	BigDecimal comboPrice,
	Instant startAt,
	Instant endAt,
	boolean isActive,
	Instant createdAt,
	Instant updatedAt,
	List<ComboOfferItemResponse> items
) {

	public static ComboOfferResponse from(ComboOfferEntity combo) {
		return new ComboOfferResponse(
			combo.getId(),
			combo.getName(),
			combo.getCode(),
			combo.getBarcode(),
			combo.getDescription(),
			combo.getComboPrice(),
			combo.getStartAt(),
			combo.getEndAt(),
			combo.isActive(),
			combo.getCreatedAt(),
			combo.getUpdatedAt(),
			combo.getItems() == null ? List.of() : combo.getItems().stream().map(ComboOfferItemResponse::from).toList()
		);
	}
}