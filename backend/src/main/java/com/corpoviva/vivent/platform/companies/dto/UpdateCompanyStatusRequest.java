package com.corpoviva.vivent.platform.companies.dto;

import com.corpoviva.vivent.companies.entity.CompanyStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCompanyStatusRequest(
	@NotNull CompanyStatus status
) {
}
