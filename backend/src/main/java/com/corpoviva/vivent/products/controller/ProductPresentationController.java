package com.corpoviva.vivent.products.controller;

import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.products.dto.ProductPresentationResponse;
import com.corpoviva.vivent.products.dto.UpsertProductPresentationRequest;
import com.corpoviva.vivent.products.service.ProductPresentationService;
import com.corpoviva.vivent.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/{productId}/presentations")
public class ProductPresentationController {

	private final ProductPresentationService productPresentationService;

	public ProductPresentationController(ProductPresentationService productPresentationService) {
		this.productPresentationService = productPresentationService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('product_presentations.view')")
	public ApiResponse<List<ProductPresentationResponse>> listPresentations(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID productId
	) {
		return ApiResponse.success(productPresentationService.listPresentations(user, productId));
	}

	@GetMapping("/{presentationId}")
	@PreAuthorize("hasAuthority('product_presentations.view')")
	public ApiResponse<ProductPresentationResponse> getPresentation(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID productId,
		@PathVariable UUID presentationId
	) {
		return ApiResponse.success(productPresentationService.getPresentation(user, productId, presentationId));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('product_presentations.create')")
	public ApiResponse<ProductPresentationResponse> createPresentation(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID productId,
		@Valid @RequestBody UpsertProductPresentationRequest request
	) {
		return ApiResponse.success(productPresentationService.createPresentation(user, productId, request));
	}

	@PutMapping("/{presentationId}")
	@PreAuthorize("hasAuthority('product_presentations.update')")
	public ApiResponse<ProductPresentationResponse> updatePresentation(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID productId,
		@PathVariable UUID presentationId,
		@Valid @RequestBody UpsertProductPresentationRequest request
	) {
		return ApiResponse.success(productPresentationService.updatePresentation(user, productId, presentationId, request));
	}

	@DeleteMapping("/{presentationId}")
	@PreAuthorize("hasAuthority('product_presentations.deactivate')")
	public ApiResponse<Void> deletePresentation(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID productId,
		@PathVariable UUID presentationId
	) {
		productPresentationService.deletePresentation(user, productId, presentationId);
		return ApiResponse.success(null);
	}
}
