package com.corpoviva.vivent.reports.dto;

import java.util.List;

public record ProductsReportResponse(
	List<ProductReportItemResponse> topSelling,
	List<ProductReportItemResponse> mostMoved,
	List<ProductReportItemResponse> noMovement,
	List<ProductReportItemResponse> lowStock,
	List<ProductReportItemResponse> outOfStock
) {
}
