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
@Table(name = "product_presentations")
public class ProductPresentationEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id", nullable = false)
	private CompanyEntity company;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private ProductEntity product;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "barcode")
	private String barcode;

	@Column(name = "conversion_factor", nullable = false)
	private BigDecimal conversionFactor;

	@Column(name = "purchase_price_suggested", nullable = false)
	private BigDecimal purchasePriceSuggested;

	@Column(name = "sale_price", nullable = false)
	private BigDecimal salePrice;

	@Column(name = "is_default_purchase", nullable = false)
	private boolean defaultPurchase;

	@Column(name = "is_default_sale", nullable = false)
	private boolean defaultSale;

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
