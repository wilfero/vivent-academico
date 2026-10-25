package com.corpoviva.vivent.inventory.entity;

import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.branches.entity.LocationEntity;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.products.entity.ProductEntity;
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
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory_balances")
public class InventoryBalanceEntity {

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

	@Column(name = "quantity_on_hand", nullable = false)
	private BigDecimal quantityOnHand;

	@Column(name = "quantity_reserved", nullable = false)
	private BigDecimal quantityReserved;

	@Column(name = "quantity_available", insertable = false, updatable = false)
	private BigDecimal quantityAvailable;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
