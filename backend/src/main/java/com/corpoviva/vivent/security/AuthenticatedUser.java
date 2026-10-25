package com.corpoviva.vivent.security;

import java.security.Principal;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedUser(
	UUID companyId,
	UUID branchId,
	String branchName,
	UUID userId,
	String tenantSlug,
	String username,
	String roleCode,
	Set<String> permissions,
	boolean mustChangePassword,
	int authVersion,
	Instant tokenExpiresAt
) implements Principal {

	public AuthenticatedUser {
		permissions = Set.copyOf(new LinkedHashSet<>(permissions));
	}

	@Override
	public String getName() {
		return username;
	}

	public Collection<? extends GrantedAuthority> authorities() {
		return permissions.stream().map(SimpleGrantedAuthority::new).toList();
	}
}
