package com.corpoviva.vivent.auth.dto;

import com.corpoviva.vivent.security.AuthenticatedUser;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CurrentSessionResponse(
	UUID companyId,
	UUID branchId,
	String branchName,
	UUID userId,
	String tenantSlug,
	String username,
	String roleCode,
	Set<String> permissions,
	boolean mustChangePassword,
	Instant tokenExpiresAt
) {

	public static CurrentSessionResponse from(AuthenticatedUser authenticatedUser) {
		return new CurrentSessionResponse(
			authenticatedUser.companyId(),
			authenticatedUser.branchId(),
			authenticatedUser.branchName(),
			authenticatedUser.userId(),
			authenticatedUser.tenantSlug(),
			authenticatedUser.username(),
			authenticatedUser.roleCode(),
			authenticatedUser.permissions(),
			authenticatedUser.mustChangePassword(),
			authenticatedUser.tokenExpiresAt()
		);
	}
}
