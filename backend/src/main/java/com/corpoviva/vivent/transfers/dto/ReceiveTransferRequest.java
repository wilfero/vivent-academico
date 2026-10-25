package com.corpoviva.vivent.transfers.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReceiveTransferRequest(
	@NotNull @Size(min = 1) List<@Valid ReceiveTransferDetailRequest> details
) {}
