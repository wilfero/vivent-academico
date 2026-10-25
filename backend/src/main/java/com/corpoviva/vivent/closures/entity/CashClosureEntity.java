package com.corpoviva.vivent.closures.entity;

import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.users.entity.UserEntity;
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
@Table(name = "cash_closures")
public class CashClosureEntity {

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
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@Column(name = "closure_date", nullable = false)
	private LocalDate closureDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private CashClosureStatus status;

	@Column(name = "confirmed_sales_total", nullable = false)
	private BigDecimal confirmedSalesTotal;

	@Column(name = "cancelled_sales_total", nullable = false)
	private BigDecimal cancelledSalesTotal;

	@Column(name = "discount_total", nullable = false)
	private BigDecimal discountTotal;

	@Column(name = "expected_total", nullable = false)
	private BigDecimal expectedTotal;

	@Column(name = "counted_total", nullable = false)
	private BigDecimal countedTotal;

	@Column(name = "difference_amount", nullable = false)
	private BigDecimal differenceAmount;

	@Column(name = "notes")
	private String notes;

	@Column(name = "approved_by")
	private UUID approvedBy;

	@Column(name = "totals_calculated_at")
	private Instant totalsCalculatedAt;

	@Column(name = "timezone_snapshot", nullable = false)
	private String timezoneSnapshot;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "closed_at")
	private Instant closedAt;

	@Column(name = "approved_at")
	private Instant approvedAt;
}
