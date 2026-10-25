package com.corpoviva.vivent.integrations.exchange.controller;

import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.integrations.exchange.dto.ExchangeRateResponse;
import com.corpoviva.vivent.integrations.exchange.service.ExchangeRateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** External integration endpoint: reference exchange rate (open.er-api.com). */
@RestController
@RequestMapping("/api/v1/integrations")
public class ExchangeRateController {

	private final ExchangeRateService exchangeRateService;

	public ExchangeRateController(ExchangeRateService exchangeRateService) {
		this.exchangeRateService = exchangeRateService;
	}

	@GetMapping("/exchange-rate")
	public ApiResponse<ExchangeRateResponse> exchangeRate(
		@RequestParam(value = "base", required = false, defaultValue = "USD") String base,
		@RequestParam(value = "target", required = false, defaultValue = "GTQ") String target
	) {
		return ApiResponse.success(exchangeRateService.lookup(base, target));
	}
}
