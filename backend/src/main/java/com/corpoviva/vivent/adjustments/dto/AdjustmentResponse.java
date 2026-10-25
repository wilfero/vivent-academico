package com.corpoviva.vivent.adjustments.dto;

import com.corpoviva.vivent.adjustments.entity.AdjustmentDirection;
import com.corpoviva.vivent.adjustments.entity.AdjustmentStatus;
import com.corpoviva.vivent.adjustments.entity.AdjustmentType;
import com.corpoviva.vivent.adjustments.entity.InventoryAdjustmentEntity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AdjustmentResponse(
	UUID id,
	UUID branchId,
	String branchName,
	UUID locationId,
	String locationName,
	String adjustmentNumber,
	AdjustmentStatus status,
	AdjustmentType adjustmentType,
	AdjustmentDirection direction,
	String notes,
	UUID createdBy,
	String createdByUsername,
	UUID approvedBy,
	String approvedByUsername,
	UUID rejectedBy,
	String rejectedByUsername,
	Instant createdAt,
	Instant updatedAt,
	Instant approvedAt,
	Instant rejectedAt,
	String rejectionReason,
	String timezoneSnapshot,
	List<AdjustmentLineResponse> lines
) {

	public static AdjustmentResponse from(InventoryAdjustmentEntity adjustment, List<AdjustmentLineResponse> lines) {
		return from(adjustment, lines, Map.of());
	}

	public static AdjustmentResponse from(InventoryAdjustmentEntity adjustment, List<AdjustmentLineResponse> lines, Map<UUID, String> usernames) {
		return new AdjustmentResponse(
			adjustment.getId(),
			adjustment.getBranch().getId(),
			adjustment.getBranch().getName(),
			adjustment.getLocation().getId(),
			adjustment.getLocation().getName(),
			adjustment.getAdjustmentNumber(),
			adjustment.getStatus(),
			adjustment.getAdjustmentType(),
			adjustment.getDirection(),
			adjustment.getNotes(),
			adjustment.getCreatedBy(),
			usernames.get(adjustment.getCreatedBy()),
			adjustment.getApprovedBy(),
			usernames.get(adjustment.getApprovedBy()),
			adjustment.getRejectedBy(),
			usernames.get(adjustment.getRejectedBy()),
			adjustment.getCreatedAt(),
			adjustment.getUpdatedAt(),
			adjustment.getApprovedAt(),
			adjustment.getRejectedAt(),
			adjustment.getRejectionReason(),
			adjustment.getTimezoneSnapshot(),
			lines
		);
	}
}
