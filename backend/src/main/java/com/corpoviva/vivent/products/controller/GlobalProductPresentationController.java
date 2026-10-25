package com.corpoviva.vivent.products.controller;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.products.dto.ProductPresentationResponse;
import com.corpoviva.vivent.products.service.ProductPresentationService;
import com.corpoviva.vivent.security.AuthenticatedUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/product-presentations")
public class GlobalProductPresentationController {

	private final ProductPresentationService productPresentationService;

	public GlobalProductPresentationController(ProductPresentationService productPresentationService) {
		this.productPresentationService = productPresentationService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('product_presentations.view')")
	public ApiResponse<PageResponse<ProductPresentationResponse>> listPresentations(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) String query,
		@RequestParam(required = false) Boolean active,
		@RequestParam(required = false) UUID productId
	) {
		return ApiResponse.success(productPresentationService.listPresentations(user, page, pageSize, query, active, productId));
	}
}
