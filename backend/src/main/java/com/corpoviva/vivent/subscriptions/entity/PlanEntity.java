package com.corpoviva.vivent.subscriptions.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "plans")
public class PlanEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "code", nullable = false)
	private String code;

	@Column(name = "description")
	private String description;

	@Column(name = "price", nullable = false)
	private BigDecimal price;

	@Column(name = "currency_code", nullable = false)
	private String currencyCode;

	@Column(name = "max_branches", nullable = false)
	private int maxBranches;

	@Column(name = "max_users", nullable = false)
	private int maxUsers;

	@Column(name = "max_products", nullable = false)
	private int maxProducts;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "enabled_modules", nullable = false, columnDefinition = "text[]")
	private String[] enabledModules;

	@Column(name = "is_custom", nullable = false)
	private boolean custom;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
