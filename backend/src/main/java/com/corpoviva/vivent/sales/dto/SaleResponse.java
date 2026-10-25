package com.corpoviva.vivent.sales.dto;

import com.corpoviva.vivent.combos.entity.SaleComboLineEntity;
import com.corpoviva.vivent.sales.entity.SaleEntity;
import com.corpoviva.vivent.sales.entity.SalePaymentMethod;
import com.corpoviva.vivent.sales.entity.SaleStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SaleResponse(
	UUID id,
	UUID branchId,
	String branchName,
	UUID customerId,
	String customerName,
	String saleNumber,
	Instant saleDate,
	BigDecimal subtotal,
	BigDecimal discountAmount,
	BigDecimal taxAmount,
	BigDecimal totalAmount,
	SalePaymentMethod paymentMethod,
	SaleStatus status,
	String notes,
	UUID createdBy,
	String createdByUsername,
	UUID cancelledBy,
	String cancelledByUsername,
	String cancelReason,
	String cancelDisposition,
	String timezoneSnapshot,
	Instant createdAt,
	Instant updatedAt,
	Instant cancelledAt,
	List<SaleDetailResponse> details,
	List<ComboLineResponse> comboLines
) {

	public static SaleResponse from(SaleEntity sale, List<SaleDetailResponse> details) {
		return from(sale, details, Map.of(), List.of());
	}

	public static SaleResponse from(SaleEntity sale, List<SaleDetailResponse> details, Map<UUID, String> usernames) {
		return from(sale, details, usernames, List.of());
	}

	public static SaleResponse from(SaleEntity sale, List<SaleDetailResponse> details, Map<UUID, String> usernames, List<SaleComboLineEntity> comboLines) {
		return new SaleResponse(
			sale.getId(),
			sale.getBranch().getId(),
			sale.getBranch().getName(),
			sale.getCustomer() == null ? null : sale.getCustomer().getId(),
			sale.getCustomer() == null ? null : sale.getCustomer().getName(),
			sale.getSaleNumber(),
			sale.getSaleDate(),
			sale.getSubtotal(),
			sale.getDiscountAmount(),
			sale.getTaxAmount(),
			sale.getTotalAmount(),
			sale.getPaymentMethod(),
			sale.getStatus(),
			sale.getNotes(),
			sale.getCreatedBy(),
			usernames.get(sale.getCreatedBy()),
			sale.getCancelledBy(),
			usernames.get(sale.getCancelledBy()),
			sale.getCancelReason(),
			sale.getCancelDisposition(),
			sale.getTimezoneSnapshot(),
			sale.getCreatedAt(),
			sale.getUpdatedAt(),
			sale.getCancelledAt(),
			details,
			comboLines.stream().map(ComboLineResponse::from).toList()
		);
	}

	public record ComboLineResponse(
		UUID id,
		UUID comboOfferId,
		String comboName,
		String comboBarcode,
		BigDecimal quantity,
		BigDecimal comboUnitPrice,
		BigDecimal totalAmount
	) {
		public static ComboLineResponse from(SaleComboLineEntity entity) {
			return new ComboLineResponse(
				entity.getId(),
				entity.getComboOffer().getId(),
				entity.getComboName(),
				entity.getComboBarcode(),
				entity.getQuantity(),
				entity.getComboUnitPrice(),
				entity.getTotalAmount()
			);
		}
	}
}
