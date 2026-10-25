package com.corpoviva.vivent.sales.controller;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.sales.dto.CancelSaleRequest;
import com.corpoviva.vivent.sales.dto.SaleResponse;
import com.corpoviva.vivent.sales.dto.UpsertSaleRequest;
import com.corpoviva.vivent.sales.entity.SalePaymentMethod;
import com.corpoviva.vivent.sales.entity.SaleStatus;
import com.corpoviva.vivent.sales.service.SaleService;
import com.corpoviva.vivent.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {

	private final SaleService saleService;

	public SaleController(SaleService saleService) {
		this.saleService = saleService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('sales.view')")
	public ApiResponse<PageResponse<SaleResponse>> listSales(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) UUID branchId,
		@RequestParam(required = false) UUID customerId,
		@RequestParam(required = false) SaleStatus status,
		@RequestParam(required = false) SalePaymentMethod paymentMethod,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
		@RequestParam(required = false) String query
	) {
		return ApiResponse.success(saleService.listSales(user, page, pageSize, branchId, customerId, status, paymentMethod, fromDate, toDate, query));
	}

	@GetMapping("/{saleId}")
	@PreAuthorize("hasAuthority('sales.view')")
	public ApiResponse<SaleResponse> getSale(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID saleId) {
		return ApiResponse.success(saleService.getSale(user, saleId));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('sales.create')")
	public ApiResponse<SaleResponse> createSale(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody UpsertSaleRequest request) {
		return ApiResponse.success(saleService.createSale(user, request));
	}

	@PutMapping("/{saleId}")
	@PreAuthorize("hasAuthority('sales.create')")
	public ApiResponse<SaleResponse> updateSale(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID saleId,
		@Valid @RequestBody UpsertSaleRequest request
	) {
		return ApiResponse.success(saleService.updateSale(user, saleId, request));
	}

	@PostMapping("/{saleId}/confirm")
	@PreAuthorize("hasAuthority('sales.create')")
	public ApiResponse<SaleResponse> confirmSale(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID saleId) {
		return ApiResponse.success(saleService.confirmSale(user, saleId));
	}

	@PostMapping("/{saleId}/cancel")
	@PreAuthorize("hasAuthority('sales.cancel')")
	public ApiResponse<SaleResponse> cancelSale(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID saleId,
		@Valid @RequestBody CancelSaleRequest request
	) {
		return ApiResponse.success(saleService.cancelSale(user, saleId, request));
	}

	@DeleteMapping("/drafts")
	@PreAuthorize("hasAuthority('sales.create')")
	public ApiResponse<Long> deleteDraftSales(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(required = false) UUID branchId
	) {
		return ApiResponse.success(saleService.deleteDraftSales(user, branchId));
	}

	@DeleteMapping("/{saleId}")
	@PreAuthorize("hasAuthority('sales.create')")
	public ApiResponse<Void> deleteDraftSale(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID saleId) {
		saleService.deleteDraftSale(user, saleId);
		return ApiResponse.success(null);
	}
}
