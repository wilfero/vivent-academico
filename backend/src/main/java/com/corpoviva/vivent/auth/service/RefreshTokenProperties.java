package com.corpoviva.vivent.auth.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "vivent.security.refresh-token")
public record RefreshTokenProperties(Duration ttl) {

	public RefreshTokenProperties {
		if (ttl == null) {
			ttl = Duration.ofDays(7);
		}
	}
}
