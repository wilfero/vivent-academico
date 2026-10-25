package com.corpoviva.vivent.tenant.dto;

import java.util.List;
import java.util.UUID;

public record PublicTenantConfigResponse(
	UUID companyId,
	String tenantSlug,
	String commercialName,
	String logoUrl,
	String primaryColor,
	String secondaryColor,
	String defaultTheme,
	List<String> supportedLocales,
	String defaultLocale,
	String currencyCode,
	String timezone,
	String subscriptionStatus,
	List<String> enabledModules
) {
}
