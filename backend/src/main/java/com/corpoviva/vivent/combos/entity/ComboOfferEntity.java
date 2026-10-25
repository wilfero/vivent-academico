package com.corpoviva.vivent.combos.entity;

import com.corpoviva.vivent.companies.entity.CompanyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "combo_offers")
public class ComboOfferEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id", nullable = false)
	private CompanyEntity company;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "code", nullable = false)
	private String code;

	@Column(name = "barcode")
	private String barcode;

	@Column(name = "description")
	private String description;

	@Column(name = "combo_price", nullable = false)
	private BigDecimal comboPrice;

	@Column(name = "start_at")
	private Instant startAt;

	@Column(name = "end_at")
	private Instant endAt;

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

	@OneToMany(mappedBy = "comboOffer", fetch = FetchType.LAZY)
	private List<ComboOfferItemEntity> items = new ArrayList<>();
}