package com.corpoviva.vivent.sales.entity;

import com.corpoviva.vivent.products.entity.ProductEntity;
import com.corpoviva.vivent.products.entity.ProductPresentationEntity;
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
@Table(name = "sale_details")
public class SaleDetailEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sale_id", nullable = false)
	private SaleEntity sale;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private ProductEntity product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "presentation_id", nullable = false)
	private ProductPresentationEntity presentation;

	@Column(name = "barcode")
	private String barcode;

	@Column(name = "quantity_presentation", nullable = false)
	private BigDecimal quantityPresentation;

	@Column(name = "conversion_factor", nullable = false)
	private BigDecimal conversionFactor;

	@Column(name = "quantity_base", nullable = false)
	private BigDecimal quantityBase;

	@Column(name = "unit_price", nullable = false)
	private BigDecimal unitPrice;

	@Column(name = "discount_amount", nullable = false)
	private BigDecimal discountAmount;

	@Column(name = "tax_amount", nullable = false)
	private BigDecimal taxAmount;

	@Column(name = "total_amount", nullable = false)
	private BigDecimal totalAmount;

	@Column(name = "fifo_cost_total", nullable = false)
	private BigDecimal fifoCostTotal;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false)
	private SaleDetailSourceType sourceType;

	@Column(name = "combo_line_id")
	private UUID comboLineId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
