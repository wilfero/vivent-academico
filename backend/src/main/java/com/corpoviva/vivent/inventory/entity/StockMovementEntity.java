package com.corpoviva.vivent.inventory.entity;

import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.branches.entity.LocationEntity;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.products.entity.ProductEntity;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_movements")
public class StockMovementEntity {

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
	@JoinColumn(name = "location_id", nullable = false)
	private LocationEntity location;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private ProductEntity product;

	@Enumerated(EnumType.STRING)
	@Column(name = "movement_type", nullable = false)
	private StockMovementType movementType;

	@Column(name = "source_type", nullable = false)
	private String sourceType;

	@Column(name = "source_id", nullable = false)
	private UUID sourceId;

	@Column(name = "quantity_in", nullable = false)
	private BigDecimal quantityIn;

	@Column(name = "quantity_out", nullable = false)
	private BigDecimal quantityOut;

	@Column(name = "unit_cost")
	private BigDecimal unitCost;

	@Column(name = "total_cost")
	private BigDecimal totalCost;

	@Column(name = "balance_after")
	private BigDecimal balanceAfter;

	@Column(name = "notes")
	private String notes;

	@Column(name = "created_by")
	private UUID createdBy;

	@Column(name = "timezone_snapshot", nullable = false)
	private String timezoneSnapshot;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
