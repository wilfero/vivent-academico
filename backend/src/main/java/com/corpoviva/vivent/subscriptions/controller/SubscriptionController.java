package com.corpoviva.vivent.subscriptions.controller;

import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.subscriptions.dto.SubscriptionResponse;
import com.corpoviva.vivent.subscriptions.service.SubscriptionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

	private final SubscriptionService subscriptionService;

	public SubscriptionController(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}

	@GetMapping("/current")
	@PreAuthorize("hasAuthority('settings.view')")
	public ApiResponse<SubscriptionResponse> getCurrentSubscription(
		@AuthenticationPrincipal AuthenticatedUser authenticatedUser
	) {
		return ApiResponse.success(subscriptionService.getCurrentSubscription(authenticatedUser));
	}
}
