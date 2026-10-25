package com.corpoviva.vivent.products.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_units")
public class ProductUnitEntity {

	@Id
	@Column(name = "code", nullable = false, length = 30)
	private String code;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "translation_key", nullable = false)
	private String translationKey;

	@Column(name = "allows_decimal", nullable = false)
	private boolean allowsDecimal;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
