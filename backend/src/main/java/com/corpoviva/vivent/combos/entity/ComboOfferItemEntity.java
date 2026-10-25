package com.corpoviva.vivent.combos.entity;

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
@Table(name = "combo_offer_items")
public class ComboOfferItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "combo_offer_id", nullable = false)
	private ComboOfferEntity comboOffer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private ProductEntity product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "presentation_id", nullable = false)
	private ProductPresentationEntity presentation;

	@Column(name = "quantity_presentation", nullable = false)
	private BigDecimal quantityPresentation;

	@Column(name = "conversion_factor", nullable = false)
	private BigDecimal conversionFactor;

	@Column(name = "quantity_base", nullable = false)
	private BigDecimal quantityBase;

	@Column(name = "regular_unit_price_snapshot", nullable = false)
	private BigDecimal regularUnitPriceSnapshot;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}