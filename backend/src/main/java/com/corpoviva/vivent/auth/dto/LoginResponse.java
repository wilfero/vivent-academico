package com.corpoviva.vivent.auth.dto;

import com.corpoviva.vivent.security.AuthenticatedUser;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record LoginResponse(
	String accessToken,
	String tokenType,
	Instant expiresAt,
	UUID companyId,
	UUID branchId,
	String branchName,
	UUID userId,
	String tenantSlug,
	String username,
	String roleCode,
	Set<String> permissions,
	boolean mustChangePassword
) {

	public static LoginResponse from(String accessToken, Instant expiresAt, AuthenticatedUser authenticatedUser) {
		return new LoginResponse(
			accessToken,
			"Bearer",
			expiresAt,
			authenticatedUser.companyId(),
			authenticatedUser.branchId(),
			authenticatedUser.branchName(),
			authenticatedUser.userId(),
			authenticatedUser.tenantSlug(),
			authenticatedUser.username(),
			authenticatedUser.roleCode(),
			authenticatedUser.permissions(),
			authenticatedUser.mustChangePassword()
		);
	}
}
