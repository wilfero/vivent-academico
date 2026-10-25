package com.corpoviva.vivent.companies.controller;

import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.companies.dto.CompanyResponse;
import com.corpoviva.vivent.companies.dto.UpdateCompanyRequest;
import com.corpoviva.vivent.companies.service.CompanyService;
import com.corpoviva.vivent.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

	private final CompanyService companyService;

	public CompanyController(CompanyService companyService) {
		this.companyService = companyService;
	}

	@GetMapping("/current")
	@PreAuthorize("hasAuthority('settings.view')")
	public ApiResponse<CompanyResponse> getCurrentCompany(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
		return ApiResponse.success(companyService.getCurrentCompany(authenticatedUser));
	}

	@PutMapping("/current")
	@PreAuthorize("hasAuthority('settings.update')")
	public ApiResponse<CompanyResponse> updateCurrentCompany(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
		@Valid @RequestBody UpdateCompanyRequest request
	) {
		return ApiResponse.success(companyService.updateCurrentCompany(authenticatedUser, request));
	}
}
