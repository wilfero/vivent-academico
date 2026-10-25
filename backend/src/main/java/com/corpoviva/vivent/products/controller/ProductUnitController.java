package com.corpoviva.vivent.products.controller;

import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.products.dto.ProductUnitResponse;
import com.corpoviva.vivent.products.dto.UpsertProductUnitRequest;
import com.corpoviva.vivent.products.service.ProductUnitService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/product-units")
public class ProductUnitController {

	private final ProductUnitService productUnitService;

	public ProductUnitController(ProductUnitService productUnitService) {
		this.productUnitService = productUnitService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('units.view')")
	public ApiResponse<List<ProductUnitResponse>> listUnits(@RequestParam(defaultValue = "false") boolean includeInactive) {
		return ApiResponse.success(productUnitService.listUnits(includeInactive));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('units.create')")
	public ApiResponse<ProductUnitResponse> createUnit(@Valid @RequestBody UpsertProductUnitRequest request) {
		return ApiResponse.success(productUnitService.createUnit(request));
	}

	@PutMapping("/{code}")
	@PreAuthorize("hasAuthority('units.update')")
	public ApiResponse<ProductUnitResponse> updateUnit(
		@PathVariable String code,
		@Valid @RequestBody UpsertProductUnitRequest request
	) {
		return ApiResponse.success(productUnitService.updateUnit(code, request));
	}

	@DeleteMapping("/{code}")
	@PreAuthorize("hasAuthority('units.deactivate')")
	public ApiResponse<Void> deactivateUnit(@PathVariable String code) {
		productUnitService.deactivateUnit(code);
		return ApiResponse.success(null);
	}
}
