package com.corpoviva.vivent.subscriptions.service;

import com.corpoviva.vivent.branches.repository.BranchRepository;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.products.repository.ProductRepository;
import com.corpoviva.vivent.subscriptions.entity.PlanEntity;
import com.corpoviva.vivent.subscriptions.entity.SubscriptionEntity;
import com.corpoviva.vivent.subscriptions.entity.SubscriptionStatus;
import com.corpoviva.vivent.subscriptions.repository.SubscriptionRepository;
import com.corpoviva.vivent.users.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionPolicyService {

	private final SubscriptionRepository subscriptionRepository;
	private final BranchRepository branchRepository;
	private final UserRepository userRepository;
	private final ProductRepository productRepository;

	public SubscriptionPolicyService(
		SubscriptionRepository subscriptionRepository,
		BranchRepository branchRepository,
		UserRepository userRepository,
		ProductRepository productRepository
	) {
		this.subscriptionRepository = subscriptionRepository;
		this.branchRepository = branchRepository;
		this.userRepository = userRepository;
		this.productRepository = productRepository;
	}

	@Transactional(readOnly = true)
	public SubscriptionEntity getValidatedCurrentSubscription(UUID companyId) {
		SubscriptionEntity subscription = subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(companyId)
			.orElseThrow(() -> ApiException.forbidden(ErrorCode.SUBSCRIPTION_EXPIRED, "Subscription is not available"));

		Instant now = Instant.now();
		boolean allowedStatus = subscription.getStatus() == SubscriptionStatus.TRIAL
			|| subscription.getStatus() == SubscriptionStatus.ACTIVE;
		boolean expiredTrial = subscription.getStatus() == SubscriptionStatus.TRIAL
			&& subscription.getTrialEndsAt() != null
			&& subscription.getTrialEndsAt().isBefore(now);
		boolean expiredActive = subscription.getStatus() == SubscriptionStatus.ACTIVE
			&& subscription.getEndsAt() != null
			&& subscription.getEndsAt().isBefore(now);

		if (!allowedStatus || expiredTrial || expiredActive) {
			throw ApiException.forbidden(ErrorCode.SUBSCRIPTION_EXPIRED, "Subscription is expired or blocked");
		}

		return subscription;
	}

	@Transactional(readOnly = true)
	public void ensureBranchCapacity(UUID companyId) {
		PlanEntity plan = getValidatedCurrentSubscription(companyId).getPlan();
		long activeBranches = branchRepository.countActiveByCompanyId(companyId);

		if (activeBranches >= plan.getMaxBranches()) {
			throw ApiException.forbidden(ErrorCode.PLAN_LIMIT_REACHED, "The subscription branch limit has been reached");
		}
	}

	@Transactional(readOnly = true)
	public void ensureUserCapacity(UUID companyId) {
		PlanEntity plan = getValidatedCurrentSubscription(companyId).getPlan();
		long activeUsers = userRepository.countActiveByCompanyId(companyId);

		if (activeUsers >= plan.getMaxUsers()) {
			throw ApiException.forbidden(ErrorCode.PLAN_LIMIT_REACHED, "The subscription user limit has been reached");
		}
	}

	@Transactional(readOnly = true)
	public void ensureProductCapacity(UUID companyId) {
		PlanEntity plan = getValidatedCurrentSubscription(companyId).getPlan();
		long activeProducts = productRepository.countActiveByCompanyId(companyId);

		if (activeProducts >= plan.getMaxProducts()) {
			throw ApiException.forbidden(ErrorCode.PLAN_LIMIT_REACHED, "The subscription product limit has been reached");
		}
	}
}
