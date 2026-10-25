package com.corpoviva.vivent.platform.auth.dto;

import com.corpoviva.vivent.platform.security.PlatformPrincipal;
import java.time.Instant;
import java.util.UUID;

public record PlatformSessionResponse(
	UUID userId,
	String username,
	String displayName,
	Instant expiresAt
) {

	public static PlatformSessionResponse from(PlatformPrincipal principal) {
		return new PlatformSessionResponse(
			principal.userId(),
			principal.username(),
			principal.displayName(),
			principal.sessionExpiresAt()
		);
	}
}
