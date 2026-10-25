package com.corpoviva.vivent.integrations.geocoding.service;

import com.corpoviva.vivent.integrations.geocoding.dto.GeocodingResult;
import tools.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Second external integration (academic deliverable): Photon geocoding.
 * Turns a free-text address into coordinates with real HTTP calls, a short TTL
 * cache and explicit success/error handling (empty query, no match, service
 * unavailable).
 */
@Service
public class GeocodingService {

	private static final String SOURCE = "PHOTON";
	private static final Duration CACHE_TTL = Duration.ofMinutes(10);
	private static final int CACHE_MAX_ENTRIES = 300;
	private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);

	private final RestClient restClient;
	private final String baseUrl;
	private final Map<String, CachedEntry> cache = new ConcurrentHashMap<>();

	public GeocodingService(
		@Value("${vivent.integrations.photon.base-url:https://photon.komoot.io}") String baseUrl,
		@Value("${vivent.integrations.photon.timeout-millis:4000}") long timeoutMillis
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

	public GeocodingResult lookup(String query, int limit) {
		String normalized = query == null ? "" : query.trim();

		if (normalized.isEmpty()) {
			return GeocodingResult.notFound(normalized, "Address query is required");
		}

		String cacheKey = normalized.toLowerCase() + "|" + limit;
		CachedEntry cached = cache.get(cacheKey);
		Instant now = Instant.now();

		if (cached != null && now.isBefore(cached.expiresAt())) {
			return cached.result();
		}

		GeocodingResult result = requestExternal(normalized, limit);
		remember(cacheKey, result);
		return result;
	}

	private GeocodingResult requestExternal(String query, int limit) {
		try {
			URI uri = UriComponentsBuilder.fromUriString(baseUrl)
				.path("/api/")
				.queryParam("q", query)
				.queryParam("limit", Math.max(1, Math.min(limit, 5)))
				.build()
				.encode()
				.toUri();

			JsonNode body = restClient.get().uri(uri).retrieve().body(JsonNode.class);

			if (body == null) {
				return GeocodingResult.notFound(query, "External geocoding returned no response");
			}

			JsonNode features = body.path("features");

			if (!features.isArray() || features.isEmpty()) {
				return GeocodingResult.notFound(query, "No match found for the address");
			}

			JsonNode first = features.get(0);
			JsonNode coordinates = first.path("geometry").path("coordinates");

			if (!coordinates.isArray() || coordinates.size() < 2) {
				return GeocodingResult.notFound(query, "No usable coordinates in the response");
			}

			JsonNode properties = first.path("properties");
			String label = buildLabel(properties);

			return new GeocodingResult(
				query,
				SOURCE,
				true,
				coordinates.get(1).asDouble(),
				coordinates.get(0).asDouble(),
				label,
				null
			);
		} catch (RuntimeException exception) {
			log.warn("Photon geocoding call failed for query '{}': {}", query, exception.toString());
			return GeocodingResult.notFound(query, "External geocoding is unavailable right now");
		}
	}

	private String buildLabel(JsonNode properties) {
		StringBuilder label = new StringBuilder();

		for (String field : new String[] { "name", "street", "city", "state", "country" }) {
			String value = properties.path(field).asText("").trim();

			if (!value.isEmpty()) {
				if (!label.isEmpty()) {
					label.append(", ");
				}
				label.append(value);
			}
		}

		return label.isEmpty() ? null : label.toString();
	}

	private void remember(String key, GeocodingResult result) {
		if (cache.size() >= CACHE_MAX_ENTRIES) {
			cache.clear();
		}
		cache.put(key, new CachedEntry(result, Instant.now().plus(CACHE_TTL)));
	}

	private record CachedEntry(GeocodingResult result, Instant expiresAt) {
	}
}
