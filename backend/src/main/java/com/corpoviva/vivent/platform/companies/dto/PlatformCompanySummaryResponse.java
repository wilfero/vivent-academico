package com.corpoviva.vivent.platform.companies.dto;

import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.entity.CompanyStatus;
import java.time.Instant;
import java.util.UUID;

public record PlatformCompanySummaryResponse(
	UUID id,
	String legalName,
	String commercialName,
	String tenantSlug,
	String domain,
	CompanyStatus status,
	String planCode,
	String planName,
	String subscriptionStatus,
	Instant createdAt
) {

	public static PlatformCompanySummaryResponse from(
		CompanyEntity company,
		String planCode,
		String planName,
		String subscriptionStatus
	) {
		return new PlatformCompanySummaryResponse(
			company.getId(),
			company.getLegalName(),
			company.getCommercialName(),
			company.getTenantSlug(),
			company.getDomain(),
			company.getStatus(),
			planCode,
			planName,
			subscriptionStatus,
			company.getCreatedAt()
		);
	}
}
