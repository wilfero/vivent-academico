package com.corpoviva.vivent.inventory.controller;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.inventory.dto.InventoryBalanceResponse;
import com.corpoviva.vivent.inventory.dto.InventoryLayerResponse;
import com.corpoviva.vivent.inventory.dto.InventorySummaryResponse;
import com.corpoviva.vivent.inventory.dto.StockInRequest;
import com.corpoviva.vivent.inventory.dto.StockMovementResponse;
import com.corpoviva.vivent.inventory.dto.StockOperationResponse;
import com.corpoviva.vivent.inventory.dto.StockOutRequest;
import com.corpoviva.vivent.inventory.entity.InventoryLayerStatus;
import com.corpoviva.vivent.inventory.entity.StockMovementType;
import com.corpoviva.vivent.inventory.service.InventoryService;
import com.corpoviva.vivent.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

	private final InventoryService inventoryService;

	public InventoryController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@GetMapping("/balances")
	@PreAuthorize("hasAuthority('inventory.view')")
	public ApiResponse<PageResponse<InventoryBalanceResponse>> listBalances(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) UUID branchId,
		@RequestParam(required = false) UUID locationId,
		@RequestParam(required = false) UUID productId,
		@RequestParam(required = false) UUID categoryId,
		@RequestParam(required = false) String query
	) {
		return ApiResponse.success(inventoryService.listBalances(user, page, pageSize, branchId, locationId, productId, categoryId, query));
	}

	@GetMapping("/summary")
	@PreAuthorize("hasAuthority('inventory.view')")
	public ApiResponse<InventorySummaryResponse> getSummary(@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(inventoryService.getSummary(user));
	}

	@GetMapping("/layers")
	@PreAuthorize("hasAuthority('inventory.view_layers')")
	public ApiResponse<PageResponse<InventoryLayerResponse>> listLayers(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) UUID branchId,
		@RequestParam(required = false) UUID locationId,
		@RequestParam(required = false) UUID productId,
		@RequestParam(required = false) InventoryLayerStatus status
	) {
		return ApiResponse.success(inventoryService.listLayers(user, page, pageSize, branchId, locationId, productId, status));
	}

	@GetMapping("/kardex")
	@PreAuthorize("hasAuthority('inventory.view_kardex')")
	public ApiResponse<PageResponse<StockMovementResponse>> getKardex(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) UUID branchId,
		@RequestParam(required = false) UUID locationId,
		@RequestParam(required = false) UUID productId,
		@RequestParam(required = false) StockMovementType movementType,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate
	) {
		return ApiResponse.success(inventoryService.getKardex(user, page, pageSize, branchId, locationId, productId, movementType, fromDate, toDate));
	}

	@GetMapping("/movements")
	@PreAuthorize("hasAuthority('inventory.view_movements')")
	public ApiResponse<PageResponse<StockMovementResponse>> listMovements(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) UUID branchId,
		@RequestParam(required = false) UUID locationId,
		@RequestParam(required = false) UUID productId,
		@RequestParam(required = false) StockMovementType movementType,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate
	) {
		return ApiResponse.success(inventoryService.listMovements(user, page, pageSize, branchId, locationId, productId, movementType, fromDate, toDate));
	}

	@PostMapping("/stock-in")
	@PreAuthorize("hasAuthority('inventory.adjust')")
	public ApiResponse<StockOperationResponse> stockIn(
		@AuthenticationPrincipal AuthenticatedUser user,
		@Valid @RequestBody StockInRequest request
	) {
		return ApiResponse.success(inventoryService.stockIn(user, request));
	}

	@PostMapping("/stock-out")
	@PreAuthorize("hasAuthority('inventory.adjust')")
	public ApiResponse<StockOperationResponse> stockOut(
		@AuthenticationPrincipal AuthenticatedUser user,
		@Valid @RequestBody StockOutRequest request
	) {
		return ApiResponse.success(inventoryService.stockOut(user, request));
	}
}
