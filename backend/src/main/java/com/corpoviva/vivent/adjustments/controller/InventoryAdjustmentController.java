package com.corpoviva.vivent.adjustments.controller;

import com.corpoviva.vivent.adjustments.dto.AdjustmentResponse;
import com.corpoviva.vivent.adjustments.dto.CancelAdjustmentRequest;
import com.corpoviva.vivent.adjustments.dto.RejectAdjustmentRequest;
import com.corpoviva.vivent.adjustments.dto.UpsertAdjustmentRequest;
import com.corpoviva.vivent.adjustments.entity.AdjustmentStatus;
import com.corpoviva.vivent.adjustments.service.InventoryAdjustmentService;
import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.security.AuthenticatedUser;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/inventory/adjustments")
public class InventoryAdjustmentController {

	private final InventoryAdjustmentService adjustmentService;

	public InventoryAdjustmentController(InventoryAdjustmentService adjustmentService) {
		this.adjustmentService = adjustmentService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('inventory.view')")
	public ApiResponse<PageResponse<AdjustmentResponse>> listAdjustments(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) UUID branchId,
		@RequestParam(required = false) AdjustmentStatus status,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
		@RequestParam(required = false) String query
	) {
		return ApiResponse.success(adjustmentService.listAdjustments(user, page, pageSize, branchId, status, fromDate, toDate, query));
	}

	@GetMapping("/{adjustmentId}")
	@PreAuthorize("hasAuthority('inventory.view')")
	public ApiResponse<AdjustmentResponse> getAdjustment(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID adjustmentId) {
		return ApiResponse.success(adjustmentService.getAdjustment(user, adjustmentId));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('inventory.adjust')")
	public ApiResponse<AdjustmentResponse> createAdjustment(
		@AuthenticationPrincipal AuthenticatedUser user,
		@Valid @RequestBody UpsertAdjustmentRequest request
	) {
		return ApiResponse.success(adjustmentService.createAdjustment(user, request));
	}

	@PutMapping("/{adjustmentId}")
	@PreAuthorize("hasAuthority('inventory.adjust')")
	public ApiResponse<AdjustmentResponse> updateAdjustment(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID adjustmentId,
		@Valid @RequestBody UpsertAdjustmentRequest request
	) {
		return ApiResponse.success(adjustmentService.updateAdjustment(user, adjustmentId, request));
	}

	@PostMapping("/{adjustmentId}/approve")
	@PreAuthorize("hasAuthority('inventory.adjust_approve')")
	public ApiResponse<AdjustmentResponse> approveAdjustment(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID adjustmentId) {
		return ApiResponse.success(adjustmentService.approveAdjustment(user, adjustmentId));
	}

	@PostMapping("/{adjustmentId}/reject")
	@PreAuthorize("hasAuthority('inventory.adjust_approve')")
	public ApiResponse<AdjustmentResponse> rejectAdjustment(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID adjustmentId,
		@Valid @RequestBody RejectAdjustmentRequest request
	) {
		return ApiResponse.success(adjustmentService.rejectAdjustment(user, adjustmentId, request));
	}

	@PostMapping("/{adjustmentId}/cancel")
	@PreAuthorize("hasAuthority('inventory.adjust')")
	public ApiResponse<AdjustmentResponse> cancelAdjustment(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID adjustmentId,
		@Valid @RequestBody CancelAdjustmentRequest request
	) {
		return ApiResponse.success(adjustmentService.cancelAdjustment(user, adjustmentId, request));
	}
}
