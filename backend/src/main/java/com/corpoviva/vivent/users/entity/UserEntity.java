package com.corpoviva.vivent.users.entity;

import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.permissions.entity.RoleEntity;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.Instant;
import jakarta.persistence.CascadeType;
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
@Table(name = "users")
public class UserEntity {

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
	@JoinColumn(name = "role_id", nullable = false)
	private RoleEntity role;

	@Column(name = "username", nullable = false)
	private String username;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "must_change_password", nullable = false)
	private boolean mustChangePassword;

	@Column(name = "auth_version", nullable = false)
	private int authVersion;

	@Column(name = "first_name", nullable = false)
	private String firstName;

	@Column(name = "last_name", nullable = false)
	private String lastName;

	@Column(name = "email", nullable = false)
	private String email;

	@Column(name = "phone")
	private String phone;

	@Column(name = "birth_date")
	private LocalDate birthDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private UserStatus status;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<UserPermissionOverrideEntity> permissionOverrides = new LinkedHashSet<>();
}
