package com.corpoviva.vivent.purchases.controller;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.purchases.dto.PurchaseResponse;
import com.corpoviva.vivent.purchases.dto.UpsertPurchaseRequest;
import com.corpoviva.vivent.purchases.dto.VoidPurchaseRequest;
import com.corpoviva.vivent.purchases.entity.PurchaseStatus;
import com.corpoviva.vivent.purchases.service.PurchaseService;
import com.corpoviva.vivent.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import java.time.LocalDate;
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
@RequestMapping("/api/v1/purchases")
public class PurchaseController {

	private final PurchaseService purchaseService;

	public PurchaseController(PurchaseService purchaseService) {
		this.purchaseService = purchaseService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('purchases.view')")
	public ApiResponse<PageResponse<PurchaseResponse>> listPurchases(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) UUID branchId,
		@RequestParam(required = false) UUID supplierId,
		@RequestParam(required = false) PurchaseStatus status,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
		@RequestParam(required = false) String query
	) {
		return ApiResponse.success(purchaseService.listPurchases(user, page, pageSize, branchId, supplierId, status, fromDate, toDate, query));
	}

	@GetMapping("/{purchaseId}")
	@PreAuthorize("hasAuthority('purchases.view')")
	public ApiResponse<PurchaseResponse> getPurchase(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID purchaseId) {
		return ApiResponse.success(purchaseService.getPurchase(user, purchaseId));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('purchases.create')")
	public ApiResponse<PurchaseResponse> createPurchase(
		@AuthenticationPrincipal AuthenticatedUser user,
		@Valid @RequestBody UpsertPurchaseRequest request
	) {
		return ApiResponse.success(purchaseService.createPurchase(user, request));
	}

	@PutMapping("/{purchaseId}")
	@PreAuthorize("hasAuthority('purchases.create')")
	public ApiResponse<PurchaseResponse> updatePurchase(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID purchaseId,
		@Valid @RequestBody UpsertPurchaseRequest request
	) {
		return ApiResponse.success(purchaseService.updatePurchase(user, purchaseId, request));
	}

	@PostMapping("/{purchaseId}/confirm")
	@PreAuthorize("hasAuthority('purchases.confirm')")
	public ApiResponse<PurchaseResponse> confirmPurchase(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID purchaseId) {
		return ApiResponse.success(purchaseService.confirmPurchase(user, purchaseId));
	}

	@PostMapping("/{purchaseId}/void")
	@PreAuthorize("hasAuthority('purchases.void')")
	public ApiResponse<PurchaseResponse> voidPurchase(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID purchaseId,
		@Valid @RequestBody VoidPurchaseRequest request
	) {
		return ApiResponse.success(purchaseService.voidPurchase(user, purchaseId, request));
	}
}
