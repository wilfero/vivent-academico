package com.corpoviva.vivent.tenant.repository;

import java.util.UUID;

public interface PublicTenantConfigProjection {

	UUID getCompanyId();

	String getTenantSlug();

	String getCommercialName();

	String getLogoUrl();

	String getPrimaryColor();

	String getSecondaryColor();

	String getDefaultTheme();

	String getSupportedLocalesJson();

	String getDefaultLocale();

	String getCurrencyCode();

	String getTimezone();

	String getSubscriptionStatus();

	String getEnabledModulesJson();
}
