package com.corpoviva.vivent.combos.entity;

import com.corpoviva.vivent.sales.entity.SaleEntity;
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
@Table(name = "sale_combo_lines")
public class SaleComboLineEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sale_id", nullable = false)
	private SaleEntity sale;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "combo_offer_id", nullable = false)
	private ComboOfferEntity comboOffer;

	@Column(name = "combo_name", nullable = false)
	private String comboName;

	@Column(name = "combo_barcode")
	private String comboBarcode;

	@Column(name = "quantity", nullable = false)
	private BigDecimal quantity;

	@Column(name = "combo_unit_price", nullable = false)
	private BigDecimal comboUnitPrice;

	@Column(name = "total_amount", nullable = false)
	private BigDecimal totalAmount;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}