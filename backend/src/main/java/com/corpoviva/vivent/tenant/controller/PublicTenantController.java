package com.corpoviva.vivent.tenant.controller;

import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.tenant.dto.PublicTenantConfigResponse;
import com.corpoviva.vivent.tenant.service.PublicTenantConfigService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/public")
public class PublicTenantController {

	private final PublicTenantConfigService publicTenantConfigService;

	public PublicTenantController(PublicTenantConfigService publicTenantConfigService) {
		this.publicTenantConfigService = publicTenantConfigService;
	}

	@GetMapping("/tenant-config")
	public ApiResponse<PublicTenantConfigResponse> getTenantConfig(@RequestParam("subdomain") @NotBlank String subdomain) {
		return ApiResponse.success(publicTenantConfigService.getTenantConfig(subdomain));
	}
}
