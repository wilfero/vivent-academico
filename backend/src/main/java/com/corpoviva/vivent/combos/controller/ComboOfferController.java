package com.corpoviva.vivent.combos.controller;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.combos.dto.ComboOfferResponse;
import com.corpoviva.vivent.combos.dto.UpsertComboOfferRequest;
import com.corpoviva.vivent.combos.service.ComboOfferService;
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
@RequestMapping("/api/v1/combos")
public class ComboOfferController {

	private final ComboOfferService comboOfferService;

	public ComboOfferController(ComboOfferService comboOfferService) {
		this.comboOfferService = comboOfferService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('combos.view')")
	public ApiResponse<PageResponse<ComboOfferResponse>> listCombos(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) Boolean active,
		@RequestParam(required = false) String query
	) {
		return ApiResponse.success(comboOfferService.listCombos(user, page, pageSize, active, query));
	}

	@GetMapping("/{comboId}")
	@PreAuthorize("hasAuthority('combos.view')")
	public ApiResponse<ComboOfferResponse> getCombo(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID comboId) {
		return ApiResponse.success(comboOfferService.getCombo(user, comboId));
	}

	@GetMapping("/by-barcode/{barcode}")
	@PreAuthorize("hasAuthority('combos.view')")
	public ApiResponse<ComboOfferResponse> getComboByBarcode(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String barcode) {
		return ApiResponse.success(comboOfferService.getComboByBarcode(user, barcode));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('combos.create')")
	public ApiResponse<ComboOfferResponse> createCombo(
		@AuthenticationPrincipal AuthenticatedUser user,
		@Valid @RequestBody UpsertComboOfferRequest request
	) {
		return ApiResponse.success(comboOfferService.createCombo(user, request));
	}

	@PutMapping("/{comboId}")
	@PreAuthorize("hasAuthority('combos.update')")
	public ApiResponse<ComboOfferResponse> updateCombo(
		@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID comboId,
		@Valid @RequestBody UpsertComboOfferRequest request
	) {
		return ApiResponse.success(comboOfferService.updateCombo(user, comboId, request));
	}

	@DeleteMapping("/{comboId}")
	@PreAuthorize("hasAuthority('combos.delete')")
	public ApiResponse<Void> deleteCombo(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID comboId) {
		comboOfferService.deleteCombo(user, comboId);
		return ApiResponse.success(null);
	}
}