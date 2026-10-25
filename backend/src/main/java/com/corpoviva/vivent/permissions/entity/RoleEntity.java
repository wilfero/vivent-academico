package com.corpoviva.vivent.permissions.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
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
@Table(name = "roles")
public class RoleEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "company_id")
	private UUID companyId;

	@Column(name = "code", nullable = false)
	private String code;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "translation_key", nullable = false)
	private String translationKey;

	@Column(name = "description")
	private String description;

	@Column(name = "is_system", nullable = false)
	private boolean system;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "role_permissions",
		joinColumns = @JoinColumn(name = "role_id"),
		inverseJoinColumns = @JoinColumn(name = "permission_id")
	)
	private Set<PermissionEntity> permissions = new LinkedHashSet<>();
}
