package com.corpoviva.vivent.subscriptions.entity;

import jakarta.persistence.FetchType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "subscriptions")
public class SubscriptionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "company_id", nullable = false)
	private UUID companyId;

	@Column(name = "plan_id", nullable = false)
	private UUID planId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "plan_id", insertable = false, updatable = false)
	private PlanEntity plan;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private SubscriptionStatus status;

	@Column(name = "starts_at", nullable = false)
	private Instant startsAt;

	@Column(name = "ends_at")
	private Instant endsAt;

	@Column(name = "trial_ends_at")
	private Instant trialEndsAt;

	@Column(name = "notes")
	private String notes;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
