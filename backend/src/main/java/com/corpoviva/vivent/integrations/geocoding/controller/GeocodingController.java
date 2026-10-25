package com.corpoviva.vivent.integrations.geocoding.controller;

import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.integrations.geocoding.dto.GeocodingResult;
import com.corpoviva.vivent.integrations.geocoding.service.GeocodingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** External integration endpoint: address -> coordinates (Photon). */
@RestController
@RequestMapping("/api/v1/integrations")
public class GeocodingController {

	private final GeocodingService geocodingService;

	public GeocodingController(GeocodingService geocodingService) {
		this.geocodingService = geocodingService;
	}

	@GetMapping("/geocode")
	public ApiResponse<GeocodingResult> geocode(
		@RequestParam("q") String query,
		@RequestParam(value = "limit", required = false, defaultValue = "1") int limit
	) {
		return ApiResponse.success(geocodingService.lookup(query, limit));
	}
}
