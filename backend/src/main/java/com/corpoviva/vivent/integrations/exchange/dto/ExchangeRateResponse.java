package com.corpoviva.vivent.integrations.exchange.dto;

import java.math.BigDecimal;

/** Result of an external exchange-rate lookup. Failures are reported with available=false. */
public record ExchangeRateResponse(
	String baseCurrency,
	String targetCurrency,
	boolean available,
	BigDecimal rate,
	String lastUpdatedUtc,
	String source,
	String message
) {

	public static ExchangeRateResponse unavailable(String base, String target, String message) {
		return new ExchangeRateResponse(base, target, false, null, null, "OPEN_ER_API", message);
	}
}
