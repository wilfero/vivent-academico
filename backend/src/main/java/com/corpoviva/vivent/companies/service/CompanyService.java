package com.corpoviva.vivent.companies.service;

import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.companies.dto.CompanyResponse;
import com.corpoviva.vivent.companies.dto.UpdateCompanyRequest;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.security.AuthenticatedUser;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

	private final CompanyRepository companyRepository;

	public CompanyService(CompanyRepository companyRepository) {
		this.companyRepository = companyRepository;
	}

	@Transactional(readOnly = true)
	public CompanyResponse getCurrentCompany(AuthenticatedUser authenticatedUser) {
		return CompanyResponse.from(loadCompany(authenticatedUser));
	}

	@Transactional
	public CompanyResponse updateCurrentCompany(AuthenticatedUser authenticatedUser, UpdateCompanyRequest request) {
		CompanyEntity company = loadCompany(authenticatedUser);
		LinkedHashSet<String> locales = validateLocaleConfiguration(request);

		company.setLegalName(request.legalName().trim());
		company.setCommercialName(request.commercialName().trim());
		company.setLogoUrl(trimToNull(request.logoUrl()));
		company.setPrimaryColor(request.primaryColor());
		company.setSecondaryColor(request.secondaryColor());
		company.setDefaultTheme(request.defaultTheme());
		company.setSupportedLocales(locales.toArray(String[]::new));
		company.setDefaultLocale(request.defaultLocale().trim());
		company.setCurrencyCode(request.currencyCode().trim().toUpperCase());
		company.setTimezone(request.timezone().trim());
		company.setTaxEnabled(request.taxEnabled());
		company.setTaxRate(request.taxRate());

		return CompanyResponse.from(company);
	}

	private CompanyEntity loadCompany(AuthenticatedUser authenticatedUser) {
		return companyRepository.findByIdAndDeletedAtIsNull(authenticatedUser.companyId())
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Company not found"));
	}

	private LinkedHashSet<String> validateLocaleConfiguration(UpdateCompanyRequest request) {
		LinkedHashSet<String> locales = new LinkedHashSet<>();
		request.supportedLocales().stream().map(String::trim).forEach(locales::add);

		if (!locales.contains(request.defaultLocale().trim())) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "defaultLocale must be included in supportedLocales");
		}

		if (locales.isEmpty()) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "supportedLocales cannot be empty");
		}

		return locales;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
