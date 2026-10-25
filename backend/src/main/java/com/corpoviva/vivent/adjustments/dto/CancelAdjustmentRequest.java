package com.corpoviva.vivent.adjustments.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelAdjustmentRequest(
	@NotBlank String reason
) {}
