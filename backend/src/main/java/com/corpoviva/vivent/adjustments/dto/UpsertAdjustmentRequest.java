package com.corpoviva.vivent.adjustments.dto;

import com.corpoviva.vivent.adjustments.entity.AdjustmentDirection;
import com.corpoviva.vivent.adjustments.entity.AdjustmentStatus;
import com.corpoviva.vivent.adjustments.entity.AdjustmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpsertAdjustmentRequest(
	UUID branchId,
	UUID locationId,
	String adjustmentNumber,
	@NotNull AdjustmentType adjustmentType,
	@NotNull AdjustmentDirection direction,
	AdjustmentStatus status,
	String notes,
	@NotNull @Size(min = 1) List<@Valid AdjustmentLineRequest> lines
) {}
