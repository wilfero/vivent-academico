package com.corpoviva.vivent.purchases.entity;

import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "purchases")
public class PurchaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id", nullable = false)
	private CompanyEntity company;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "branch_id", nullable = false)
	private BranchEntity branch;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id", nullable = false)
	private SupplierEntity supplier;

	@Column(name = "purchase_number", nullable = false)
	private String purchaseNumber;

	@Column(name = "supplier_invoice_number")
	private String supplierInvoiceNumber;

	@Column(name = "purchase_date", nullable = false)
	private LocalDate purchaseDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false)
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_type", nullable = false)
	private PaymentType paymentType;

	@Column(name = "subtotal", nullable = false)
	private BigDecimal subtotal;

	@Column(name = "tax_amount", nullable = false)
	private BigDecimal taxAmount;

	@Column(name = "discount_amount", nullable = false)
	private BigDecimal discountAmount;

	@Column(name = "total_amount", nullable = false)
	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private PurchaseStatus status;

	@Column(name = "notes")
	private String notes;

	@Column(name = "created_by", nullable = false)
	private UUID createdBy;

	@Column(name = "confirmed_by")
	private UUID confirmedBy;

	@Column(name = "voided_by")
	private UUID voidedBy;

	@Column(name = "void_reason")
	private String voidReason;

	@Column(name = "timezone_snapshot", nullable = false)
	private String timezoneSnapshot;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "confirmed_at")
	private Instant confirmedAt;

	@Column(name = "voided_at")
	private Instant voidedAt;
}
