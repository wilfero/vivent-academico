package com.corpoviva.vivent.platform.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "vivent.platform")
public record PlatformProperties(
	@NotBlank String cookieName,
	boolean cookieSecure,
	@NotNull Duration sessionTtl,
	@Min(1) int maxFailedAttempts,
	@NotNull Duration lockDuration,
	Bootstrap bootstrap
) {

	public record Bootstrap(
		boolean enabled,
		String username,
		String password
	) {
	}
}
