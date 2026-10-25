package com.corpoviva.vivent.platform.companies.controller;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.companies.entity.CompanyStatus;
import com.corpoviva.vivent.platform.companies.dto.CreatePlatformCompanyRequest;
import com.corpoviva.vivent.platform.companies.dto.PlatformCompanyResponse;
import com.corpoviva.vivent.platform.companies.dto.PlatformCompanySummaryResponse;
import com.corpoviva.vivent.platform.companies.dto.ProvisionedCompanyResponse;
import com.corpoviva.vivent.platform.companies.dto.ResetAdminPasswordResponse;
import com.corpoviva.vivent.platform.companies.dto.UpdateCompanyStatusRequest;
import com.corpoviva.vivent.platform.companies.dto.UpdatePlatformCompanyRequest;
import com.corpoviva.vivent.platform.companies.dto.UpdateSubscriptionRequest;
import com.corpoviva.vivent.platform.companies.service.PlatformCompanyProvisioningService;
import com.corpoviva.vivent.platform.companies.service.PlatformCompanyService;
import com.corpoviva.vivent.platform.security.PlatformPrincipal;
import com.corpoviva.vivent.subscriptions.dto.SubscriptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform/companies")
public class PlatformCompanyController {

	private final PlatformCompanyService platformCompanyService;
	private final PlatformCompanyProvisioningService platformCompanyProvisioningService;

	public PlatformCompanyController(
		PlatformCompanyService platformCompanyService,
		PlatformCompanyProvisioningService platformCompanyProvisioningService
	) {
		this.platformCompanyService = platformCompanyService;
		this.platformCompanyProvisioningService = platformCompanyProvisioningService;
	}

	@GetMapping
	public ApiResponse<PageResponse<PlatformCompanySummaryResponse>> listCompanies(
		@RequestParam(name = "page", defaultValue = "1") int page,
		@RequestParam(name = "pageSize", defaultValue = "20") int pageSize,
		@RequestParam(name = "query", required = false) String query,
		@RequestParam(name = "status", required = false) CompanyStatus status
	) {
		return ApiResponse.success(platformCompanyService.listCompanies(page, pageSize, query, status));
	}

	@PostMapping
	public ApiResponse<ProvisionedCompanyResponse> createCompany(
		@Valid @RequestBody CreatePlatformCompanyRequest request,
		@AuthenticationPrincipal PlatformPrincipal principal,
		HttpServletRequest httpRequest
	) {
		return ApiResponse.success(platformCompanyProvisioningService.provision(
			request,
			principal.userId(),
			httpRequest.getRemoteAddr(),
			httpRequest.getHeader("User-Agent")
		));
	}

	@GetMapping("/{companyId}")
	public ApiResponse<PlatformCompanyResponse> getCompany(@PathVariable UUID companyId) {
		return ApiResponse.success(platformCompanyService.getCompany(companyId));
	}

	@PutMapping("/{companyId}")
	public ApiResponse<PlatformCompanyResponse> updateCompany(
		@PathVariable UUID companyId,
		@Valid @RequestBody UpdatePlatformCompanyRequest request,
		@AuthenticationPrincipal PlatformPrincipal principal,
		HttpServletRequest httpRequest
	) {
		return ApiResponse.success(platformCompanyService.updateCompany(
			companyId,
			request,
			principal.userId(),
			httpRequest.getRemoteAddr(),
			httpRequest.getHeader("User-Agent")
		));
	}

	@PutMapping("/{companyId}/status")
	public ApiResponse<PlatformCompanySummaryResponse> updateStatus(
		@PathVariable UUID companyId,
		@Valid @RequestBody UpdateCompanyStatusRequest request,
		@AuthenticationPrincipal PlatformPrincipal principal,
		HttpServletRequest httpRequest
	) {
		return ApiResponse.success(platformCompanyService.updateStatus(
			companyId,
			request,
			principal.userId(),
			httpRequest.getRemoteAddr(),
			httpRequest.getHeader("User-Agent")
		));
	}

	@PutMapping("/{companyId}/subscription")
	public ApiResponse<SubscriptionResponse> updateSubscription(
		@PathVariable UUID companyId,
		@Valid @RequestBody UpdateSubscriptionRequest request,
		@AuthenticationPrincipal PlatformPrincipal principal,
		HttpServletRequest httpRequest
	) {
		return ApiResponse.success(platformCompanyService.updateSubscription(
			companyId,
			request,
			principal.userId(),
			httpRequest.getRemoteAddr(),
			httpRequest.getHeader("User-Agent")
		));
	}

	@PostMapping("/{companyId}/admin-password/reset")
	public ApiResponse<ResetAdminPasswordResponse> resetAdminPassword(
		@PathVariable UUID companyId,
		@AuthenticationPrincipal PlatformPrincipal principal,
		HttpServletRequest httpRequest
	) {
		return ApiResponse.success(platformCompanyService.resetAdminPassword(
			companyId,
			principal.userId(),
			httpRequest.getRemoteAddr(),
			httpRequest.getHeader("User-Agent")
		));
	}
}
