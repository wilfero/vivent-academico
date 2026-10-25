package com.corpoviva.vivent.subscriptions.dto;

import com.corpoviva.vivent.subscriptions.entity.SubscriptionEntity;
import com.corpoviva.vivent.subscriptions.entity.SubscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
	UUID id,
	UUID companyId,
	SubscriptionStatus status,
	Instant startsAt,
	Instant endsAt,
	Instant trialEndsAt,
	String notes,
	PlanResponse plan
) {

	public static SubscriptionResponse from(SubscriptionEntity subscription) {
		return new SubscriptionResponse(
			subscription.getId(),
			subscription.getCompanyId(),
			subscription.getStatus(),
			subscription.getStartsAt(),
			subscription.getEndsAt(),
			subscription.getTrialEndsAt(),
			subscription.getNotes(),
			PlanResponse.from(subscription.getPlan())
		);
	}
}
