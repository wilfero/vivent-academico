package com.corpoviva.vivent.adjustments.entity;

import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.branches.entity.LocationEntity;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
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
@Table(name = "inventory_adjustments")
public class InventoryAdjustmentEntity {

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

	@Column(name = "adjustment_number", nullable = false)
	private String adjustmentNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private AdjustmentStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "adjustment_type", nullable = false)
	private AdjustmentType adjustmentType;

	@Enumerated(EnumType.STRING)
	@Column(name = "direction", nullable = false)
	private AdjustmentDirection direction;

	@Column(name = "notes")
	private String notes;

	@Column(name = "created_by", nullable = false)
	private UUID createdBy;

	@Column(name = "approved_by")
	private UUID approvedBy;

	@Column(name = "rejected_by")
	private UUID rejectedBy;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "approved_at")
	private Instant approvedAt;

	@Column(name = "rejected_at")
	private Instant rejectedAt;

	@Column(name = "rejection_reason")
	private String rejectionReason;

	@Column(name = "timezone_snapshot", nullable = false)
	private String timezoneSnapshot;
}
