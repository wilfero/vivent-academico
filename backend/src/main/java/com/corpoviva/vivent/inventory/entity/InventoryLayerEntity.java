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
@Table(name = "inventory_layers")
public class InventoryLayerEntity {

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

	@Column(name = "purchase_detail_id")
	private UUID purchaseDetailId;

	@Column(name = "source_type", nullable = false)
	private String sourceType;

	@Column(name = "source_id", nullable = false)
	private UUID sourceId;

	@Column(name = "quantity_received", nullable = false)
	private BigDecimal quantityReceived;

	@Column(name = "quantity_remaining", nullable = false)
	private BigDecimal quantityRemaining;

	@Column(name = "unit_cost", nullable = false)
	private BigDecimal unitCost;

	@Column(name = "received_at", nullable = false)
	private Instant receivedAt;

	@Column(name = "expiration_date")
	private LocalDate expirationDate;

	@Column(name = "lot_number")
	private String lotNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private InventoryLayerStatus status;

	@Column(name = "timezone_snapshot", nullable = false)
	private String timezoneSnapshot;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
