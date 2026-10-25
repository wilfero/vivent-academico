package com.corpoviva.vivent.platform.companies.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreatePlatformCompanyRequest(
	@NotBlank @Size(max = 150) String legalName,
	@NotBlank @Size(max = 150) String commercialName,
	@NotBlank @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") @Size(max = 80) String tenantSlug,
	@NotNull UUID planId,
	@NotBlank @Size(max = 150) String branchName,
	@NotBlank @Pattern(regexp = "^[A-Z0-9_-]+$") @Size(max = 50) String branchCode,
	@NotBlank @Size(max = 80) String adminUsername,
	@NotBlank @Size(max = 100) String adminFirstName,
	@NotBlank @Size(max = 100) String adminLastName,
	@NotBlank @Email @Size(max = 150) String adminEmail,
	@Size(max = 500) String logoUrl,
	@Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String primaryColor,
	@Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String secondaryColor,
	@Pattern(regexp = "^(light|dark)$") String defaultTheme,
	List<@NotBlank String> supportedLocales,
	@Size(max = 20) String defaultLocale,
	@Size(min = 3, max = 3) String currencyCode,
	@Size(max = 80) String timezone,
	Boolean taxEnabled,
	@DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRate
) {
}
