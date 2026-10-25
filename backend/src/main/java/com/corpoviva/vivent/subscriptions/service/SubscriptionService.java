package com.corpoviva.vivent.subscriptions.service;

import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.subscriptions.dto.SubscriptionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

	private final SubscriptionPolicyService subscriptionPolicyService;

	public SubscriptionService(SubscriptionPolicyService subscriptionPolicyService) {
		this.subscriptionPolicyService = subscriptionPolicyService;
	}

	@Transactional(readOnly = true)
	public SubscriptionResponse getCurrentSubscription(AuthenticatedUser authenticatedUser) {
		return SubscriptionResponse.from(subscriptionPolicyService.getValidatedCurrentSubscription(authenticatedUser.companyId()));
	}
}
