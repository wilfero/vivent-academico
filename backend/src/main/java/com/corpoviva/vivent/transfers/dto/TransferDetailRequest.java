package com.corpoviva.vivent.transfers.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferDetailRequest(
	@NotNull UUID presentationId,
	@NotNull @DecimalMin(value = "0.000001") BigDecimal quantityRequestedPresentation
) {}
