package com.corpoviva.vivent.companies.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record UpdateCompanyRequest(
	@NotBlank @Size(max = 150) String legalName,
	@NotBlank @Size(max = 150) String commercialName,
	@Size(max = 500) String logoUrl,
	@NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String primaryColor,
	@NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String secondaryColor,
	@NotBlank @Pattern(regexp = "^(light|dark)$") String defaultTheme,
	@NotEmpty List<@NotBlank String> supportedLocales,
	@NotBlank String defaultLocale,
	@NotBlank @Size(min = 3, max = 3) String currencyCode,
	@NotBlank @Size(max = 80) String timezone,
	boolean taxEnabled,
	@NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRate
) {
}
