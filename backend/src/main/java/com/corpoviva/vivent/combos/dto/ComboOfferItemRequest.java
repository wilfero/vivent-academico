package com.corpoviva.vivent.combos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record ComboOfferItemRequest(
	@NotNull UUID productId,
	@NotNull UUID presentationId,
	@NotNull @Positive BigDecimal quantityPresentation
) {}