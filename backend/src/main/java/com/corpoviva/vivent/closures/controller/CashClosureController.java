package com.corpoviva.vivent.closures.controller;

import com.corpoviva.vivent.closures.dto.CashClosureCloseRequest;
import com.corpoviva.vivent.closures.dto.CashClosurePreviewResponse;
import com.corpoviva.vivent.closures.dto.CashClosureRequest;
import com.corpoviva.vivent.closures.dto.CashClosureResponse;
import com.corpoviva.vivent.closures.entity.CashClosureStatus;
import com.corpoviva.vivent.closures.service.CashClosureService;
import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/cash-closures")
public class CashClosureController {

	private final CashClosureService cashClosureService;

	public CashClosureController(CashClosureService cashClosureService) {
		this.cashClosureService = cashClosureService;
	}

	@GetMapping
	@PreAuthorize("hasAnyAuthority('closures.view','cash_closures.view','cash_closures.create','cash_closures.approve')")
	public ApiResponse<PageResponse<CashClosureResponse>> listClosures(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) UUID branchId,
		@RequestParam(required = false) CashClosureStatus status,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
	) {
		return ApiResponse.success(cashClosureService.listClosures(user, page, pageSize, branchId, status, fromDate, toDate));
	}

	@GetMapping("/preview")
	@PreAuthorize("hasAnyAuthority('closures.create','cash_closures.create','closures.view','cash_closures.view','closures.approve','cash_closures.approve')")
	public ApiResponse<CashClosurePreviewResponse> previewClosure(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam UUID branchId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate closureDate
	) {
		return ApiResponse.success(cashClosureService.previewClosure(user, branchId, closureDate));
	}

	@GetMapping("/{closureId}")
	@PreAuthorize("hasAnyAuthority('closures.view','cash_closures.view','cash_closures.create','cash_closures.approve')")
	public ApiResponse<CashClosureResponse> getClosure(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID closureId) {
		return ApiResponse.success(cashClosureService.getClosure(user, closureId));
	}

	@PostMapping
	@PreAuthorize("hasAnyAuthority('closures.create','cash_closures.create')")
	public ApiResponse<CashClosureResponse> createClosure(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CashClosureRequest request) {
		return ApiResponse.success(cashClosureService.createClosure(user, request));
	}

	@PostMapping("/{closureId}/approve")
	@PreAuthorize("hasAnyAuthority('closures.approve','cash_closures.approve')")
	public ApiResponse<CashClosureResponse> approveClosure(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID closureId) {
		return ApiResponse.success(cashClosureService.approveClosure(user, closureId));
	}

	@PostMapping("/{closureId}/reopen")
	@PreAuthorize("hasAnyAuthority('closures.approve','cash_closures.approve')")
	public ApiResponse<CashClosureResponse> reopenClosure(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID closureId) {
		return ApiResponse.success(cashClosureService.reopenClosure(user, closureId));
	}

	@PostMapping("/{closureId}/close")
	@PreAuthorize("hasAnyAuthority('closures.approve','cash_closures.approve')")
	public ApiResponse<CashClosureResponse> closeClosure(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID closureId, @Valid @RequestBody(required = false) CashClosureCloseRequest request) {
		return ApiResponse.success(cashClosureService.closeClosure(user, closureId, request));
	}
}
