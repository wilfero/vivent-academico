package com.corpoviva.vivent.sales.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelSaleRequest(
	@NotBlank String reason,
	String cancelDisposition
) {}
