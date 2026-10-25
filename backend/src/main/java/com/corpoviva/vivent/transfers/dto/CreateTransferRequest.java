package com.corpoviva.vivent.transfers.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateTransferRequest(
	@NotNull UUID sourceBranchId,
	@NotNull UUID destinationBranchId,
	String transferNumber,
	String notes,
	@NotNull @Size(min = 1) List<@Valid TransferDetailRequest> details
) {}
