package com.corpoviva.vivent.platform.plans.controller;

import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.platform.plans.repository.PlatformPlanRepository;
import com.corpoviva.vivent.subscriptions.dto.PlanResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform/plans")
public class PlatformPlanController {

	private final PlatformPlanRepository platformPlanRepository;

	public PlatformPlanController(PlatformPlanRepository platformPlanRepository) {
		this.platformPlanRepository = platformPlanRepository;
	}

	@GetMapping
	public ApiResponse<List<PlanResponse>> listPlans() {
		List<PlanResponse> plans = platformPlanRepository.findAllByActiveTrueOrderByNameAsc()
			.stream()
			.map(PlanResponse::from)
			.toList();

		return ApiResponse.success(plans);
	}
}
