package com.corpoviva.vivent.barcodes.dto;

/**
 * Result of an external barcode lookup. HTTP and external-service failures are
 * reported with found=false and a clear message instead of an API error so the
 * POS can decide how to proceed.
 */
public record BarcodeLookupResponse(
	String barcode,
	String source,
	boolean found,
	String name,
	String brand,
	String quantityText,
	String imageUrl,
	String message
) {

	public static BarcodeLookupResponse notFound(String barcode, String message) {
		return new BarcodeLookupResponse(barcode, "OPEN_FOOD_FACTS", false, null, null, null, null, message);
	}
}
