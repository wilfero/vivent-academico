package com.corpoviva.vivent.companies.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "companies")
public class CompanyEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "legal_name", nullable = false)
	private String legalName;

	@Column(name = "commercial_name", nullable = false)
	private String commercialName;

	@Column(name = "tenant_slug", nullable = false)
	private String tenantSlug;

	@Column(name = "domain", nullable = false)
	private String domain;

	@Column(name = "logo_url")
	private String logoUrl;

	@Column(name = "primary_color", nullable = false)
	private String primaryColor;

	@Column(name = "secondary_color", nullable = false)
	private String secondaryColor;

	@Column(name = "default_theme", nullable = false)
	private String defaultTheme;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "supported_locales", nullable = false, columnDefinition = "text[]")
	private String[] supportedLocales;

	@Column(name = "default_locale", nullable = false)
	private String defaultLocale;

	@Column(name = "currency_code", nullable = false)
	private String currencyCode;

	@Column(name = "timezone", nullable = false)
	private String timezone;

	@Column(name = "tax_enabled", nullable = false)
	private boolean taxEnabled;

	@Column(name = "tax_rate", nullable = false)
	private BigDecimal taxRate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private CompanyStatus status;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;
}
