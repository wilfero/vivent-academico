package com.corpoviva.vivent.purchases.dto;

import com.corpoviva.vivent.purchases.entity.PaymentMethod;
import com.corpoviva.vivent.purchases.entity.PaymentType;
import com.corpoviva.vivent.purchases.entity.PurchaseEntity;
import com.corpoviva.vivent.purchases.entity.PurchaseStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PurchaseResponse(
	UUID id,
	UUID branchId,
	String branchName,
	UUID supplierId,
	String supplierName,
	String purchaseNumber,
	String supplierInvoiceNumber,
	LocalDate purchaseDate,
	PaymentMethod paymentMethod,
	PaymentType paymentType,
	BigDecimal subtotal,
	BigDecimal taxAmount,
	BigDecimal discountAmount,
	BigDecimal totalAmount,
	PurchaseStatus status,
	String notes,
	UUID createdBy,
	String createdByUsername,
	UUID confirmedBy,
	String confirmedByUsername,
	UUID voidedBy,
	String voidedByUsername,
	String voidReason,
	String timezoneSnapshot,
	Instant createdAt,
	Instant updatedAt,
	Instant confirmedAt,
	Instant voidedAt,
	List<PurchaseDetailResponse> details
) {

	public static PurchaseResponse from(PurchaseEntity purchase, List<PurchaseDetailResponse> details) {
		return from(purchase, details, Map.of());
	}

	public static PurchaseResponse from(PurchaseEntity purchase, List<PurchaseDetailResponse> details, Map<UUID, String> usernames) {
		return new PurchaseResponse(
			purchase.getId(),
			purchase.getBranch().getId(),
			purchase.getBranch().getName(),
			purchase.getSupplier().getId(),
			purchase.getSupplier().getName(),
			purchase.getPurchaseNumber(),
			purchase.getSupplierInvoiceNumber(),
			purchase.getPurchaseDate(),
			purchase.getPaymentMethod(),
			purchase.getPaymentType(),
			purchase.getSubtotal(),
			purchase.getTaxAmount(),
			purchase.getDiscountAmount(),
			purchase.getTotalAmount(),
			purchase.getStatus(),
			purchase.getNotes(),
			purchase.getCreatedBy(),
			usernames.get(purchase.getCreatedBy()),
			purchase.getConfirmedBy(),
			usernames.get(purchase.getConfirmedBy()),
			purchase.getVoidedBy(),
			usernames.get(purchase.getVoidedBy()),
			purchase.getVoidReason(),
			purchase.getTimezoneSnapshot(),
			purchase.getCreatedAt(),
			purchase.getUpdatedAt(),
			purchase.getConfirmedAt(),
			purchase.getVoidedAt(),
			details
		);
	}
}
