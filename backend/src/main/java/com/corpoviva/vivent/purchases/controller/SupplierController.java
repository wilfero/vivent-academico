package com.corpoviva.vivent.purchases.controller;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.purchases.dto.SupplierResponse;
import com.corpoviva.vivent.purchases.dto.UpsertSupplierRequest;
import com.corpoviva.vivent.purchases.service.SupplierService;
import com.corpoviva.vivent.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

	private final SupplierService supplierService;

	public SupplierController(SupplierService supplierService) {
		this.supplierService = supplierService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('suppliers.view')")
	public ApiResponse<PageResponse<SupplierResponse>> listSuppliers(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) String query,
		@RequestParam(required = false) Boolean active
	) {
		return ApiResponse.success(supplierService.listSuppliers(user, page, pageSize, query, active));
	}

	@GetMapping("/{supplierId}")
	@PreAuthorize("hasAuthority('suppliers.view')")
	public ApiResponse<SupplierResponse> getSupplier(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID supplierId) {
		return ApiResponse.success(supplierService.getSupplier(user, supplierId));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('suppliers.create')")
	public ApiResponse<SupplierResponse> createSupplier(
		@AuthenticationPrincipal AuthenticatedUser user,
		@Valid @RequestBody UpsertSupplierRequest request
	) {
		return ApiResponse.success(supplierService.createSupplier(user, request));
	}

	@PutMapping("/{supplierId}")
	@PreAuthorize("hasAuthority('suppliers.update')")
	public ApiResponse<SupplierResponse> updateSupplier(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID supplierId,
		@Valid @RequestBody UpsertSupplierRequest request
	) {
		return ApiResponse.success(supplierService.updateSupplier(user, supplierId, request));
	}

	@DeleteMapping("/{supplierId}")
	@PreAuthorize("hasAuthority('suppliers.delete')")
	public ApiResponse<Void> deleteSupplier(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID supplierId) {
		supplierService.deleteSupplier(user, supplierId);
		return ApiResponse.success(null);
	}
}
