package com.corpoviva.vivent.products.controller;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.products.dto.BarcodeLookupResponse;
import com.corpoviva.vivent.products.dto.ProductResponse;
import com.corpoviva.vivent.products.dto.UpsertProductRequest;
import com.corpoviva.vivent.products.service.ProductPresentationService;
import com.corpoviva.vivent.products.service.ProductService;
import com.corpoviva.vivent.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/products")
public class ProductController {

	private final ProductService productService;
	private final ProductPresentationService productPresentationService;

	public ProductController(ProductService productService, ProductPresentationService productPresentationService) {
		this.productService = productService;
		this.productPresentationService = productPresentationService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('products.view')")
	public ApiResponse<PageResponse<ProductResponse>> listProducts(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) String query,
		@RequestParam(required = false) Boolean active,
		@RequestParam(required = false) UUID categoryId
	) {
		return ApiResponse.success(productService.listProducts(user, page, pageSize, query, active, categoryId));
	}

	@GetMapping("/by-barcode/{barcode}")
	@PreAuthorize("hasAuthority('products.view')")
	public ApiResponse<BarcodeLookupResponse> lookupBarcode(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String barcode) {
		return ApiResponse.success(productPresentationService.lookupBarcode(user, barcode));
	}

	@GetMapping("/{productId}")
	@PreAuthorize("hasAuthority('products.view')")
	public ApiResponse<ProductResponse> getProduct(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID productId) {
		return ApiResponse.success(productService.getProduct(user, productId));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('products.create')")
	public ApiResponse<ProductResponse> createProduct(
		@AuthenticationPrincipal AuthenticatedUser user,
		@Valid @RequestBody UpsertProductRequest request
	) {
		return ApiResponse.success(productService.createProduct(user, request));
	}

	@PutMapping("/{productId}")
	@PreAuthorize("hasAuthority('products.update')")
	public ApiResponse<ProductResponse> updateProduct(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID productId,
		@Valid @RequestBody UpsertProductRequest request
	) {
		return ApiResponse.success(productService.updateProduct(user, productId, request));
	}

	@DeleteMapping("/{productId}")
	@PreAuthorize("hasAuthority('products.delete')")
	public ApiResponse<Void> deleteProduct(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID productId) {
		productService.deleteProduct(user, productId);
		return ApiResponse.success(null);
	}
}
