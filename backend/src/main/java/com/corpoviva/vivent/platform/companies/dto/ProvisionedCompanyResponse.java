package com.corpoviva.vivent.platform.companies.dto;

import java.util.UUID;

public record ProvisionedCompanyResponse(
	UUID companyId,
	String tenantSlug,
	String domain,
	String adminUsername,
	String adminPassword
) {
}
