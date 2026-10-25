package com.corpoviva.vivent.subscriptions.dto;

import com.corpoviva.vivent.subscriptions.entity.PlanEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PlanResponse(
	UUID id,
	String name,
	String code,
	String description,
	BigDecimal price,
	String currencyCode,
	int maxBranches,
	int maxUsers,
	int maxProducts,
	List<String> enabledModules,
	boolean custom,
	boolean active
) {

	public static PlanResponse from(PlanEntity plan) {
		return new PlanResponse(
			plan.getId(),
			plan.getName(),
			plan.getCode(),
			plan.getDescription(),
			plan.getPrice(),
			plan.getCurrencyCode(),
			plan.getMaxBranches(),
			plan.getMaxUsers(),
			plan.getMaxProducts(),
			List.of(plan.getEnabledModules()),
			plan.isCustom(),
			plan.isActive()
		);
	}
}
