package com.corpoviva.vivent.branches.controller;

import com.corpoviva.vivent.branches.dto.BranchResponse;
import com.corpoviva.vivent.branches.dto.UpsertBranchRequest;
import com.corpoviva.vivent.branches.service.BranchService;
import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

	private final BranchService branchService;

	public BranchController(BranchService branchService) {
		this.branchService = branchService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('settings.view')")
	public ApiResponse<PageResponse<BranchResponse>> listBranches(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) String query,
		@RequestParam(required = false) Boolean active
	) {
		return ApiResponse.success(branchService.listBranches(authenticatedUser, page, pageSize, query, active));
	}

	@GetMapping("/{branchId}")
	@PreAuthorize("hasAuthority('settings.view')")
	public ApiResponse<BranchResponse> getBranch(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable UUID branchId
	) {
		return ApiResponse.success(branchService.getBranch(authenticatedUser, branchId));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('settings.update')")
	public ApiResponse<BranchResponse> createBranch(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody UpsertBranchRequest request
	) {
		return ApiResponse.success(branchService.createBranch(authenticatedUser, request));
	}

	@PutMapping("/{branchId}")
	@PreAuthorize("hasAuthority('settings.update')")
	public ApiResponse<BranchResponse> updateBranch(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@PathVariable UUID branchId,
		@Valid @RequestBody UpsertBranchRequest request
	) {
		return ApiResponse.success(branchService.updateBranch(authenticatedUser, branchId, request));
	}
}
