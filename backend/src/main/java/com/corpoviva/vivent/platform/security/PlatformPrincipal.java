package com.corpoviva.vivent.platform.security;

import java.security.Principal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record PlatformPrincipal(
	UUID userId,
	String username,
	String displayName,
	Instant sessionExpiresAt
) implements Principal {

	public static final String AUTHORITY = "PLATFORM_OPERATOR";

	@Override
	public String getName() {
		return username;
	}

	public Collection<? extends GrantedAuthority> authorities() {
		return List.of(new SimpleGrantedAuthority(AUTHORITY));
	}
}
