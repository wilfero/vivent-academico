package com.corpoviva.vivent.companies.dto;

import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.entity.CompanyStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CompanyResponse(
	UUID id,
	String legalName,
	String commercialName,
	String tenantSlug,
	String domain,
	String logoUrl,
	String primaryColor,
	String secondaryColor,
	String defaultTheme,
	List<String> supportedLocales,
	String defaultLocale,
	String currencyCode,
	String timezone,
	boolean taxEnabled,
	BigDecimal taxRate,
	CompanyStatus status
) {

	public static CompanyResponse from(CompanyEntity company) {
		return new CompanyResponse(
			company.getId(),
			company.getLegalName(),
			company.getCommercialName(),
			company.getTenantSlug(),
			company.getDomain(),
			company.getLogoUrl(),
			company.getPrimaryColor(),
			company.getSecondaryColor(),
			company.getDefaultTheme(),
			List.of(company.getSupportedLocales()),
			company.getDefaultLocale(),
			company.getCurrencyCode(),
			company.getTimezone(),
			company.isTaxEnabled(),
			company.getTaxRate(),
			company.getStatus()
		);
	}
}
