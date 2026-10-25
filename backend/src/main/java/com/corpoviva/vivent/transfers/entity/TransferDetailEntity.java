package com.corpoviva.vivent.transfers.entity;

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
@Table(name = "transfer_details")
public class TransferDetailEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transfer_id", nullable = false)
	private TransferEntity transfer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private ProductEntity product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "presentation_id", nullable = false)
	private ProductPresentationEntity presentation;

	@Column(name = "quantity_requested_presentation", nullable = false)
	private BigDecimal quantityRequestedPresentation;

	@Column(name = "conversion_factor", nullable = false)
	private BigDecimal conversionFactor;

	@Column(name = "quantity_requested_base", nullable = false)
	private BigDecimal quantityRequestedBase;

	@Column(name = "quantity_approved_base", nullable = false)
	private BigDecimal quantityApprovedBase;

	@Column(name = "quantity_received_base", nullable = false)
	private BigDecimal quantityReceivedBase;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
