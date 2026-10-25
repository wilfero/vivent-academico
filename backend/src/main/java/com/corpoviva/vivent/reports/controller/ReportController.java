package com.corpoviva.vivent.reports.controller;

import com.corpoviva.vivent.adjustments.entity.AdjustmentStatus;
import com.corpoviva.vivent.closures.entity.CashClosureStatus;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.purchases.entity.PurchaseStatus;
import com.corpoviva.vivent.reports.dto.AdjustmentsReportSummaryResponse;
import com.corpoviva.vivent.reports.dto.CashClosuresReportSummaryResponse;
import com.corpoviva.vivent.reports.dto.ProductsReportResponse;
import com.corpoviva.vivent.reports.dto.PurchasesReportSummaryResponse;
import com.corpoviva.vivent.reports.dto.SalesReportSummaryResponse;
import com.corpoviva.vivent.reports.dto.TransfersReportSummaryResponse;
import com.corpoviva.vivent.reports.service.ReportService;
import com.corpoviva.vivent.sales.entity.SalePaymentMethod;
import com.corpoviva.vivent.sales.entity.SaleStatus;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.transfers.entity.TransferStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

	private final ReportService reportService;

	public ReportController(ReportService reportService) {
		this.reportService = reportService;
	}

	@GetMapping("/sales-summary")
	@PreAuthorize("hasAuthority('sales.view') and hasAnyAuthority('reports.sales.view','reports.view','reports.view_branch','reports.view_company')")
	public ApiResponse<SalesReportSummaryResponse> salesSummary(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(required = false) UUID branchId,
		@RequestParam(required = false) UUID customerId,
		@RequestParam(required = false) SaleStatus status,
		@RequestParam(required = false) SalePaymentMethod paymentMethod,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
		@RequestParam(required = false) String query
	) {
		return ApiResponse.success(reportService.salesSummary(user, branchId, customerId, status, paymentMethod, fromDate, toDate, query));
	}

	@GetMapping("/purchases-summary")
	@PreAuthorize("hasAuthority('purchases.view') and hasAnyAuthority('reports.purchases.view','reports.view','reports.view_branch','reports.view_company')")
	public ApiResponse<PurchasesReportSummaryResponse> purchasesSummary(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(required = false) UUID branchId,
		@RequestParam(required = false) UUID supplierId,
		@RequestParam(required = false) PurchaseStatus status,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
		@RequestParam(required = false) String query
	) {
		return ApiResponse.success(reportService.purchasesSummary(user, branchId, supplierId, status, fromDate, toDate, query));
	}

	@GetMapping("/products")
	@PreAuthorize("hasAuthority('inventory.view') and hasAnyAuthority('reports.products.view','reports.inventory.view','reports.view','reports.view_branch','reports.view_company')")
	public ApiResponse<ProductsReportResponse> productsReport(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(required = false) UUID branchId,
		@RequestParam(required = false) UUID categoryId,
		@RequestParam(required = false) UUID productId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate
	) {
		return ApiResponse.success(reportService.productsReport(user, branchId, categoryId, productId, fromDate, toDate));
	}

	@GetMapping("/transfers-summary")
	@PreAuthorize("hasAuthority('transfers.view') and hasAnyAuthority('reports.transfers.view','reports.view','reports.view_branch','reports.view_company')")
	public ApiResponse<TransfersReportSummaryResponse> transfersSummary(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(required = false) UUID sourceBranchId,
		@RequestParam(required = false) UUID destinationBranchId,
		@RequestParam(required = false) TransferStatus status,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
		@RequestParam(required = false) String query
	) {
		return ApiResponse.success(reportService.transfersSummary(user, sourceBranchId, destinationBranchId, status, fromDate, toDate, query));
	}

	@GetMapping("/adjustments-summary")
	@PreAuthorize("hasAuthority('inventory.view') and hasAnyAuthority('reports.adjustments.view','reports.view','reports.view_branch','reports.view_company')")
	public ApiResponse<AdjustmentsReportSummaryResponse> adjustmentsSummary(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(required = false) UUID branchId,
		@RequestParam(required = false) AdjustmentStatus status,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
		@RequestParam(required = false) String query
	) {
		return ApiResponse.success(reportService.adjustmentsSummary(user, branchId, status, fromDate, toDate, query));
	}

	@GetMapping("/cash-closures-summary")
	@PreAuthorize("hasAnyAuthority('closures.view','cash_closures.view','cash_closures.create','cash_closures.approve')")
	public ApiResponse<CashClosuresReportSummaryResponse> cashClosuresSummary(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(required = false) UUID branchId,
		@RequestParam(required = false) CashClosureStatus status,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
	) {
		return ApiResponse.success(reportService.cashClosuresSummary(user, branchId, status, fromDate, toDate));
	}
}
