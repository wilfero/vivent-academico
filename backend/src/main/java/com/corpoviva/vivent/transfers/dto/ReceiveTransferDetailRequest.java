package com.corpoviva.vivent.transfers.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ReceiveTransferDetailRequest(
	@NotNull UUID detailId,
	@NotNull @DecimalMin(value = "0.00") BigDecimal quantityReceivedBase
) {}
