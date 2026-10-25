package com.corpoviva.vivent.barcodes.service;

import com.corpoviva.vivent.barcodes.dto.BarcodeLookupResponse;
import tools.jackson.databind.JsonNode;
import java.net.URI;
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
 * Third-party integration (student deliverable): Open Food Facts product
 * lookup by barcode. Performs real HTTP requests against the public API with
 * success and error handling (product not found, service unavailable) and a
 * small TTL cache to avoid repeated calls during a scanning session.
 */
@Service
public class BarcodeLookupService {

	private static final String SOURCE = "OPEN_FOOD_FACTS";
	private static final Duration CACHE_TTL = Duration.ofMinutes(10);
	private static final int CACHE_MAX_ENTRIES = 500;
	private static final Logger log = LoggerFactory.getLogger(BarcodeLookupService.class);

	private final RestClient restClient;
	private final Map<String, CachedEntry> cache = new ConcurrentHashMap<>();

	public BarcodeLookupService(
		@Value("${vivent.integrations.openfoodfacts.base-url:https://world.openfoodfacts.org}") String baseUrl,
		@Value("${vivent.integrations.openfoodfacts.timeout-millis:4000}") long timeoutMillis
	) {
		this.restClient = RestClient.builder()
			.baseUrl(URI.create(baseUrl))
			.requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
				setConnectTimeout((int) timeoutMillis);
				setReadTimeout((int) timeoutMillis);
			}})
			.defaultHeader("User-Agent", "Vivent-Academic - academic project - contact: contacto@nabrix.com.gt")
			.build();
	}

	public BarcodeLookupResponse lookup(String barcode) {
		String normalized = barcode == null ? "" : barcode.trim();

		if (normalized.isEmpty()) {
			return BarcodeLookupResponse.notFound(normalized, "Barcode is required");
		}

		CachedEntry cached = cache.get(normalized);
		Instant now = Instant.now();

		if (cached != null && now.isBefore(cached.expiresAt())) {
			return cached.response();
		}

		BarcodeLookupResponse response = requestExternal(normalized);
		remember(normalized, response);
		return response;
	}

	private BarcodeLookupResponse requestExternal(String barcode) {
		try {
			String path = "/api/v2/product/" + barcode + ".json?fields=product_name,brands,quantity,image_small_url";
			JsonNode body = restClient.get().uri(path).retrieve().body(JsonNode.class);

			if (body == null) {
				return BarcodeLookupResponse.notFound(barcode, "External lookup returned no response");
			}

			int status = body.path("status").asInt(0);

			if (status != 1 || body.path("product").isMissingNode()) {
				return BarcodeLookupResponse.notFound(barcode, "Product not found in the external catalog");
			}

			JsonNode product = body.path("product");
			String name = textOrNull(product, "product_name");

			if (name == null || name.isBlank()) {
				return BarcodeLookupResponse.notFound(barcode, "Product found without usable name");
			}

			return new BarcodeLookupResponse(
				barcode,
				SOURCE,
				true,
				name,
				textOrNull(product, "brands"),
				textOrNull(product, "quantity"),
				textOrNull(product, "image_small_url"),
				null
			);
		} catch (RuntimeException exception) {
			log.warn("Open Food Facts lookup failed for barcode '{}': {}", barcode, exception.toString());
			return BarcodeLookupResponse.notFound(barcode, "External lookup is unavailable right now");
		}
	}

	private String textOrNull(JsonNode node, String field) {
		JsonNode value = node.path(field);
		if (value.isMissingNode() || value.isNull()) {
			return null;
		}
		String text = value.asText().trim();
		return text.isEmpty() ? null : text;
	}

	private void remember(String barcode, BarcodeLookupResponse response) {
		if (cache.size() >= CACHE_MAX_ENTRIES) {
			cache.clear();
		}
		cache.put(barcode, new CachedEntry(response, Instant.now().plus(CACHE_TTL)));
	}

	private record CachedEntry(BarcodeLookupResponse response, Instant expiresAt) {
	}
}
