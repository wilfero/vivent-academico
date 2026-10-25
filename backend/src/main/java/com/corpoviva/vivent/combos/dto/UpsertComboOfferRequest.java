package com.corpoviva.vivent.combos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record UpsertComboOfferRequest(
	@NotBlank String name,
	@NotBlank String code,
	String barcode,
	String description,
	@NotNull @Positive BigDecimal comboPrice,
	Instant startAt,
	Instant endAt,
	Boolean isActive,
	List<ComboOfferItemRequest> items
) {}