package com.corpoviva.vivent.tenant.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "vivent.tenant")
public record TenantProperties(
	@NotBlank String publicBaseDomain
) {
}
