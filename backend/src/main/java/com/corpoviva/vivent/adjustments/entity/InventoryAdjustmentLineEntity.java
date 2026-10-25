package com.corpoviva.vivent.adjustments.entity;

import com.corpoviva.vivent.products.entity.ProductEntity;
import com.corpoviva.vivent.products.entity.ProductPresentationEntity;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory_adjustment_lines")
public class InventoryAdjustmentLineEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "adjustment_id", nullable = false)
	private InventoryAdjustmentEntity adjustment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private ProductEntity product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "presentation_id")
	private ProductPresentationEntity presentation;

	@Column(name = "quantity_presentation")
	private BigDecimal quantityPresentation;

	@Column(name = "conversion_factor")
	private BigDecimal conversionFactor;

	@Column(name = "quantity_base", nullable = false)
	private BigDecimal quantityBase;

	@Column(name = "unit_cost")
	private BigDecimal unitCost;

	@Column(name = "notes")
	private String notes;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
