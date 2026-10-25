package com.corpoviva.vivent.products.controller;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.products.dto.CategoryResponse;
import com.corpoviva.vivent.products.dto.UpsertCategoryRequest;
import com.corpoviva.vivent.products.service.CategoryService;
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
@RequestMapping("/api/v1/categories")
public class CategoryController {

	private final CategoryService categoryService;

	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('product_categories.view')")
	public ApiResponse<PageResponse<CategoryResponse>> listCategories(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) String query,
		@RequestParam(required = false) Boolean active
	) {
		return ApiResponse.success(categoryService.listCategories(user, page, pageSize, query, active));
	}

	@GetMapping("/{categoryId}")
	@PreAuthorize("hasAuthority('product_categories.view')")
	public ApiResponse<CategoryResponse> getCategory(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID categoryId) {
		return ApiResponse.success(categoryService.getCategory(user, categoryId));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('product_categories.create')")
	public ApiResponse<CategoryResponse> createCategory(
		@AuthenticationPrincipal AuthenticatedUser user,
		@Valid @RequestBody UpsertCategoryRequest request
	) {
		return ApiResponse.success(categoryService.createCategory(user, request));
	}

	@PutMapping("/{categoryId}")
	@PreAuthorize("hasAuthority('product_categories.update')")
	public ApiResponse<CategoryResponse> updateCategory(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID categoryId,
		@Valid @RequestBody UpsertCategoryRequest request
	) {
		return ApiResponse.success(categoryService.updateCategory(user, categoryId, request));
	}

	@DeleteMapping("/{categoryId}")
	@PreAuthorize("hasAuthority('product_categories.deactivate')")
	public ApiResponse<Void> deleteCategory(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID categoryId) {
		categoryService.deleteCategory(user, categoryId);
		return ApiResponse.success(null);
	}
}
