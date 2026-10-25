package com.corpoviva.vivent.platform.companies.dto;

import com.corpoviva.vivent.subscriptions.entity.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record UpdateSubscriptionRequest(
	@NotNull UUID planId,
	@NotNull SubscriptionStatus status,
	Instant endsAt,
	Instant trialEndsAt,
	@Size(max = 2000) String notes
) {
}
