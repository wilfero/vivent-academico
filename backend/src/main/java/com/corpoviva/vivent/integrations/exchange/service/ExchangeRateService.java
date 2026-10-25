package com.corpoviva.vivent.integrations.exchange.service;

import com.corpoviva.vivent.integrations.exchange.dto.ExchangeRateResponse;
import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Third external integration (academic deliverable): exchange rates from a
 * public API (open.er-api.com). Used as a reference for pricing/costing in a
 * GTQ-based inventory. Real HTTP calls with a one-hour cache and explicit
 * success/error handling.
 */
@Service
public class ExchangeRateService {

	private static final String SOURCE = "OPEN_ER_API";
	private static final Duration CACHE_TTL = Duration.ofHours(1);
	private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

	private final RestClient restClient;
	private final String baseUrl;
	private final Map<String, CachedEntry> cache = new ConcurrentHashMap<>();

	public ExchangeRateService(
		@Value("${vivent.integrations.exchangerate.base-url:https://open.er-api.com}") String baseUrl,
		@Value("${vivent.integrations.exchangerate.timeout-millis:4000}") long timeoutMillis
	) {
		this.baseUrl = baseUrl;
		this.restClient = RestClient.builder()
			.requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
				setConnectTimeout((int) timeoutMillis);
				setReadTimeout((int) timeoutMillis);
			}})
			.defaultHeader("User-Agent", "Vivent-Academic - academic project")
			.build();
	}

	public ExchangeRateResponse lookup(String baseCurrency, String targetCurrency) {
		String base = normalize(baseCurrency, "USD");
		String target = normalize(targetCurrency, "GTQ");
		String cacheKey = base + "|" + target;
		CachedEntry cached = cache.get(cacheKey);
		Instant now = Instant.now();

		if (cached != null && now.isBefore(cached.expiresAt())) {
			return cached.response();
		}

		ExchangeRateResponse response = requestExternal(base, target);
		cache.put(cacheKey, new CachedEntry(response, now.plus(CACHE_TTL)));
		return response;
	}

	private ExchangeRateResponse requestExternal(String base, String target) {
		try {
			JsonNode body = restClient.get()
				.uri(baseUrl + "/v6/latest/" + base)
				.retrieve()
				.body(JsonNode.class);

			if (body == null) {
				return ExchangeRateResponse.unavailable(base, target, "External exchange service returned no response");
			}

			String result = body.path("result").asText("");

			if (!"success".equalsIgnoreCase(result)) {
				return ExchangeRateResponse.unavailable(base, target, "External exchange service rejected the request");
			}

			JsonNode rateNode = body.path("rates").path(target);

			if (rateNode.isMissingNode() || rateNode.isNull()) {
				return ExchangeRateResponse.unavailable(base, target, "Target currency is not supported by the external service");
			}

			return new ExchangeRateResponse(
				base,
				target,
				true,
				new BigDecimal(rateNode.asText()),
				body.path("time_last_update_utc").asText(null),
				SOURCE,
				null
			);
		} catch (RuntimeException exception) {
			log.warn("Exchange rate call failed for {}->{}: {}", base, target, exception.toString());
			return ExchangeRateResponse.unavailable(base, target, "External exchange service is unavailable right now");
		}
	}

	private String normalize(String value, String fallback) {
		String normalized = value == null ? "" : value.trim().toUpperCase();

		if (normalized.length() != 3 || !normalized.chars().allMatch(Character::isLetter)) {
			return fallback;
		}

		return normalized;
	}

	private record CachedEntry(ExchangeRateResponse response, Instant expiresAt) {
	}
}
