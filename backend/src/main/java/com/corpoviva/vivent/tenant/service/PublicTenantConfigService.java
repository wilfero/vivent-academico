package com.corpoviva.vivent.tenant.service;

import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.entity.CompanyStatus;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.tenant.dto.PublicTenantConfigResponse;
import com.corpoviva.vivent.tenant.repository.PublicTenantConfigProjection;
import com.corpoviva.vivent.tenant.repository.PublicTenantConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicTenantConfigService {

	private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
	};

	private final CompanyRepository companyRepository;
	private final PublicTenantConfigRepository publicTenantConfigRepository;
	private final ObjectMapper objectMapper;

	public PublicTenantConfigService(
		CompanyRepository companyRepository,
		PublicTenantConfigRepository publicTenantConfigRepository,
		ObjectMapper objectMapper
	) {
		this.companyRepository = companyRepository;
		this.publicTenantConfigRepository = publicTenantConfigRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public PublicTenantConfigResponse getTenantConfig(String subdomain) {
		CompanyEntity company = companyRepository.findByTenantSlugAndDeletedAtIsNull(subdomain)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Tenant not found"));

		if (company.getStatus() != CompanyStatus.ACTIVE) {
			throw ApiException.forbidden(ErrorCode.COMPANY_INACTIVE, "Company is not active");
		}

		PublicTenantConfigProjection projection = publicTenantConfigRepository.findPublicTenantConfigByCompanyId(company.getId())
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Tenant configuration not found"));

		List<String> supportedLocales = parseStringList(projection.getSupportedLocalesJson());
		List<String> enabledModules = parseStringList(projection.getEnabledModulesJson());

		if (supportedLocales.isEmpty()) {
			supportedLocales = List.of(projection.getDefaultLocale());
		}

		return new PublicTenantConfigResponse(
			projection.getCompanyId(),
			projection.getTenantSlug(),
			projection.getCommercialName(),
			projection.getLogoUrl(),
			projection.getPrimaryColor(),
			projection.getSecondaryColor(),
			projection.getDefaultTheme(),
			supportedLocales,
			projection.getDefaultLocale(),
			projection.getCurrencyCode(),
			projection.getTimezone(),
			projection.getSubscriptionStatus(),
			enabledModules
		);
	}

	private List<String> parseStringList(String json) {
		try {
			return objectMapper.readValue(json, STRING_LIST_TYPE);
		} catch (JsonProcessingException exception) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Invalid tenant configuration payload");
		}
	}
}
