package com.corpoviva.vivent.platform.companies.dto;

import com.corpoviva.vivent.companies.dto.CompanyResponse;
import com.corpoviva.vivent.subscriptions.dto.SubscriptionResponse;
import java.util.UUID;

public record PlatformCompanyResponse(
	CompanyResponse company,
	SubscriptionResponse subscription,
	UsageResponse usage,
	AdminUserResponse admin
) {

	public record UsageResponse(
		long branches,
		long users,
		long products
	) {
	}

	public record AdminUserResponse(
		UUID userId,
		String username,
		String email,
		String firstName,
		String lastName
	) {
	}
}
