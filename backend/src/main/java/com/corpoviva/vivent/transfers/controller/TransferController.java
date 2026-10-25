package com.corpoviva.vivent.transfers.controller;

import com.corpoviva.vivent.branches.dto.BranchResponse;
import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.transfers.dto.CancelTransferRequest;
import com.corpoviva.vivent.transfers.dto.CreateTransferRequest;
import com.corpoviva.vivent.transfers.dto.ReceiveTransferRequest;
import com.corpoviva.vivent.transfers.dto.TransferResponse;
import com.corpoviva.vivent.transfers.entity.TransferStatus;
import com.corpoviva.vivent.transfers.service.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

	private final TransferService transferService;

	public TransferController(TransferService transferService) {
		this.transferService = transferService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('transfers.view')")
	public ApiResponse<PageResponse<TransferResponse>> listTransfers(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) UUID sourceBranchId,
		@RequestParam(required = false) UUID destinationBranchId,
		@RequestParam(required = false) TransferStatus status,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
		@RequestParam(required = false) String query
	) {
		return ApiResponse.success(transferService.listTransfers(user, page, pageSize, sourceBranchId, destinationBranchId, status, fromDate, toDate, query));
	}

	@GetMapping("/source-branches")
	@PreAuthorize("hasAuthority('transfers.request') or hasAuthority('transfers.view')")
	public ApiResponse<PageResponse<BranchResponse>> listSourceBranches(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "100") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) String query
	) {
		return ApiResponse.success(transferService.listSourceBranches(user, page, pageSize, query));
	}

	@GetMapping("/{transferId}")
	@PreAuthorize("hasAuthority('transfers.view')")
	public ApiResponse<TransferResponse> getTransfer(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID transferId) {
		return ApiResponse.success(transferService.getTransfer(user, transferId));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('transfers.request')")
	public ApiResponse<TransferResponse> createTransfer(
		@AuthenticationPrincipal AuthenticatedUser user,
		@Valid @RequestBody CreateTransferRequest request
	) {
		return ApiResponse.success(transferService.createTransfer(user, request));
	}

	@PostMapping("/{transferId}/approve")
	@PreAuthorize("hasAuthority('transfers.approve')")
	public ApiResponse<TransferResponse> approveTransfer(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID transferId) {
		return ApiResponse.success(transferService.approveTransfer(user, transferId));
	}

	@PostMapping("/{transferId}/reject")
	@PreAuthorize("hasAuthority('transfers.approve')")
	public ApiResponse<TransferResponse> rejectTransfer(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID transferId) {
		return ApiResponse.success(transferService.rejectTransfer(user, transferId));
	}

	@PostMapping("/{transferId}/dispatch")
	@PreAuthorize("hasAuthority('transfers.dispatch')")
	public ApiResponse<TransferResponse> dispatchTransfer(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID transferId) {
		return ApiResponse.success(transferService.dispatchTransfer(user, transferId));
	}

	@PostMapping("/{transferId}/receive")
	@PreAuthorize("hasAuthority('transfers.receive')")
	public ApiResponse<TransferResponse> receiveTransfer(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID transferId,
		@Valid @RequestBody ReceiveTransferRequest request
	) {
		return ApiResponse.success(transferService.receiveTransfer(user, transferId, request));
	}

	@PostMapping("/{transferId}/cancel")
	@PreAuthorize("hasAuthority('transfers.cancel')")
	public ApiResponse<TransferResponse> cancelTransfer(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID transferId,
		@Valid @RequestBody CancelTransferRequest request
	) {
		return ApiResponse.success(transferService.cancelTransfer(user, transferId, request));
	}
}
