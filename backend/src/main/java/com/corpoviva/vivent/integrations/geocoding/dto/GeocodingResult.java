package com.corpoviva.vivent.integrations.geocoding.dto;

/** Result of an external geocoding lookup (Photon). Failures are reported with found=false. */
public record GeocodingResult(
	String query,
	String source,
	boolean found,
	Double latitude,
	Double longitude,
	String label,
	String message
) {

	public static GeocodingResult notFound(String query, String message) {
		return new GeocodingResult(query, "PHOTON", false, null, null, null, message);
	}
}
