package com.corpoviva.vivent.sales.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record SaleComboLineRequest(
	@NotNull UUID comboOfferId,
	@NotNull @Positive BigDecimal quantity
) {}