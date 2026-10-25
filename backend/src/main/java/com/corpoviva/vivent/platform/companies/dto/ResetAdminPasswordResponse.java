package com.corpoviva.vivent.platform.companies.dto;

import java.util.UUID;

public record ResetAdminPasswordResponse(
	UUID userId,
	String username,
	String password
) {
}
