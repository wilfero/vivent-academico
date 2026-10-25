package com.corpoviva.vivent.purchases.dto;

import jakarta.validation.constraints.NotBlank;

public record VoidPurchaseRequest(
	@NotBlank String reason
) {}
