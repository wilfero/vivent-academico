package com.corpoviva.vivent.transfers.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelTransferRequest(
	@NotBlank String reason
) {}
