package com.corpoviva.vivent.purchases.dto;

import com.corpoviva.vivent.purchases.entity.PaymentMethod;
import com.corpoviva.vivent.purchases.entity.PaymentType;
import com.corpoviva.vivent.purchases.entity.PurchaseStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpsertPurchaseRequest(
	UUID branchId,
	@NotNull UUID supplierId,
	String purchaseNumber,
	String supplierInvoiceNumber,
	@NotNull LocalDate purchaseDate,
	@NotNull PaymentMethod paymentMethod,
	@NotNull PaymentType paymentType,
	PurchaseStatus status,
	@DecimalMin(value = "0.00") BigDecimal discountAmount,
	String notes,
	@NotNull @Size(min = 1) List<@Valid PurchaseDetailRequest> details
) {}
