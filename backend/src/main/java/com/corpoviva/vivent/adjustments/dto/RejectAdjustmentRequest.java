package com.corpoviva.vivent.adjustments.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectAdjustmentRequest(
	@NotBlank String reason
) {}
