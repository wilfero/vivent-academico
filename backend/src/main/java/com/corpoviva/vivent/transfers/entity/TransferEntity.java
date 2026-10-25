package com.corpoviva.vivent.transfers.entity;

import com.corpoviva.vivent.branches.entity.BranchEntity;
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
@Table(name = "transfers")
public class TransferEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id", nullable = false)
	private CompanyEntity company;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_branch_id", nullable = false)
	private BranchEntity sourceBranch;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "destination_branch_id", nullable = false)
	private BranchEntity destinationBranch;

	@Column(name = "transfer_number", nullable = false)
	private String transferNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private TransferStatus status;

	@Column(name = "requested_by", nullable = false)
	private UUID requestedBy;

	@Column(name = "approved_by")
	private UUID approvedBy;

	@Column(name = "dispatched_by")
	private UUID dispatchedBy;

	@Column(name = "received_by")
	private UUID receivedBy;

	@Column(name = "cancelled_by")
	private UUID cancelledBy;

	@Column(name = "requested_at", nullable = false)
	private Instant requestedAt;

	@Column(name = "approved_at")
	private Instant approvedAt;

	@Column(name = "dispatched_at")
	private Instant dispatchedAt;

	@Column(name = "received_at")
	private Instant receivedAt;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@Column(name = "cancel_reason")
	private String cancelReason;

	@Column(name = "notes")
	private String notes;

	@Column(name = "timezone_snapshot", nullable = false)
	private String timezoneSnapshot;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
