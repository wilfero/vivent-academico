package com.corpoviva.vivent.transfers.dto;

import com.corpoviva.vivent.transfers.entity.TransferEntity;
import com.corpoviva.vivent.transfers.entity.TransferStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransferResponse(
	UUID id,
	UUID sourceBranchId,
	String sourceBranchName,
	UUID destinationBranchId,
	String destinationBranchName,
	String transferNumber,
	TransferStatus status,
	UUID requestedBy,
	String requestedByUsername,
	UUID approvedBy,
	String approvedByUsername,
	UUID dispatchedBy,
	String dispatchedByUsername,
	UUID receivedBy,
	String receivedByUsername,
	UUID cancelledBy,
	String cancelledByUsername,
	Instant requestedAt,
	Instant approvedAt,
	Instant dispatchedAt,
	Instant receivedAt,
	Instant cancelledAt,
	String cancelReason,
	String notes,
	String timezoneSnapshot,
	List<TransferDetailResponse> details
) {

	public static TransferResponse from(TransferEntity transfer, List<TransferDetailResponse> details) {
		return from(transfer, details, java.util.Map.of());
	}

	public static TransferResponse from(TransferEntity transfer, List<TransferDetailResponse> details, java.util.Map<UUID, String> usernames) {
		return new TransferResponse(
			transfer.getId(),
			transfer.getSourceBranch().getId(),
			transfer.getSourceBranch().getName(),
			transfer.getDestinationBranch().getId(),
			transfer.getDestinationBranch().getName(),
			transfer.getTransferNumber(),
			transfer.getStatus(),
			transfer.getRequestedBy(),
			usernames.get(transfer.getRequestedBy()),
			transfer.getApprovedBy(),
			usernames.get(transfer.getApprovedBy()),
			transfer.getDispatchedBy(),
			usernames.get(transfer.getDispatchedBy()),
			transfer.getReceivedBy(),
			usernames.get(transfer.getReceivedBy()),
			transfer.getCancelledBy(),
			usernames.get(transfer.getCancelledBy()),
			transfer.getRequestedAt(),
			transfer.getApprovedAt(),
			transfer.getDispatchedAt(),
			transfer.getReceivedAt(),
			transfer.getCancelledAt(),
			transfer.getCancelReason(),
			transfer.getNotes(),
			transfer.getTimezoneSnapshot(),
			details
		);
	}
}
