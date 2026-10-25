package com.corpoviva.vivent.sales.dto;

import com.corpoviva.vivent.sales.entity.SalePaymentMethod;
import com.corpoviva.vivent.sales.entity.SaleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpsertSaleRequest(
	UUID branchId,
	UUID customerId,
	String saleNumber,
	@NotNull SalePaymentMethod paymentMethod,
	SaleStatus status,
	@DecimalMin(value = "0.00") BigDecimal discountAmount,
	String notes,
	@NotNull List<@Valid SaleDetailRequest> details,
	List<@Valid SaleComboLineRequest> comboLines
) {}
