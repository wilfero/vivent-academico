package com.corpoviva.vivent.sales.entity;

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
@Table(name = "sales")
public class SaleEntity {

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
	@JoinColumn(name = "customer_id")
	private CustomerEntity customer;

	@Column(name = "sale_number", nullable = false)
	private String saleNumber;

	@Column(name = "sale_date", nullable = false)
	private Instant saleDate;

	@Column(name = "confirmed_at")
	private Instant confirmedAt;

	@Column(name = "subtotal", nullable = false)
	private BigDecimal subtotal;

	@Column(name = "discount_amount", nullable = false)
	private BigDecimal discountAmount;

	@Column(name = "tax_amount", nullable = false)
	private BigDecimal taxAmount;

	@Column(name = "total_amount", nullable = false)
	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false)
	private SalePaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private SaleStatus status;

	@Column(name = "notes")
	private String notes;

	@Column(name = "created_by", nullable = false)
	private UUID createdBy;

	@Column(name = "cancelled_by")
	private UUID cancelledBy;

	@Column(name = "cancel_reason")
	private String cancelReason;

	@Column(name = "cancel_disposition")
	private String cancelDisposition;

	@Column(name = "timezone_snapshot", nullable = false)
	private String timezoneSnapshot;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;
}
