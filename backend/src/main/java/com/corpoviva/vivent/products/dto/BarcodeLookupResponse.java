package com.corpoviva.vivent.products.dto;

import com.corpoviva.vivent.products.entity.ProductPresentationEntity;

public record BarcodeLookupResponse(
	ProductResponse product,
	ProductPresentationResponse presentation
) {

	public static BarcodeLookupResponse from(ProductPresentationEntity presentation) {
		return new BarcodeLookupResponse(
			ProductResponse.from(presentation.getProduct()),
			ProductPresentationResponse.from(presentation)
		);
	}
}
