package com.corpoviva.vivent.permissions.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "permissions")
public class PermissionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "code", nullable = false)
	private String code;

	@Column(name = "translation_key", nullable = false)
	private String translationKey;

	@Column(name = "module_code", nullable = false)
	private String moduleCode;

	@Column(name = "action_code", nullable = false)
	private String actionCode;

	@Column(name = "description")
	private String description;

	@Column(name = "is_system", nullable = false)
	private boolean system;
}
