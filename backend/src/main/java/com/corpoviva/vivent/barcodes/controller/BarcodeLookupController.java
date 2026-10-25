package com.corpoviva.vivent.barcodes.controller;

import com.corpoviva.vivent.barcodes.dto.BarcodeLookupResponse;
import com.corpoviva.vivent.barcodes.service.BarcodeLookupService;
import com.corpoviva.vivent.common.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * External barcode lookup used as a POS fallback when the local catalog does
 * not know the scanned barcode.
 */
@RestController
@RequestMapping("/api/v1/products")
public class BarcodeLookupController {

	private final BarcodeLookupService barcodeLookupService;

	public BarcodeLookupController(BarcodeLookupService barcodeLookupService) {
		this.barcodeLookupService = barcodeLookupService;
	}

	@GetMapping("/barcode-lookup/{barcode}")
	@PreAuthorize("hasAuthority('products.view')")
	public ApiResponse<BarcodeLookupResponse> lookup(@PathVariable String barcode) {
		return ApiResponse.success(barcodeLookupService.lookup(barcode));
	}
}
