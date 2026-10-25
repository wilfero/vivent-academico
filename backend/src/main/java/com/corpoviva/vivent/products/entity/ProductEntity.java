package com.corpoviva.vivent.products.entity;

import com.corpoviva.vivent.companies.entity.CompanyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "products")
public class ProductEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id", nullable = false)
	private CompanyEntity company;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private CategoryEntity category;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "description")
	private String description;

	@Column(name = "base_unit_code", nullable = false)
	private String baseUnitCode;

	@Column(name = "sku")
	private String sku;

	@Column(name = "minimum_stock", nullable = false)
	private BigDecimal minimumStock;

	@Column(name = "is_stockable", nullable = false)
	private boolean stockable;

	@Column(name = "has_expiration_date", nullable = false)
	private boolean hasExpirationDate;

	@Column(name = "has_lot_tracking", nullable = false)
	private boolean hasLotTracking;

	@Column(name = "has_serial_tracking", nullable = false)
	private boolean hasSerialTracking;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;
}
