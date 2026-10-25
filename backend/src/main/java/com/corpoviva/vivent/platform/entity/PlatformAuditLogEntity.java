package com.corpoviva.vivent.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "platform_audit_logs")
public class PlatformAuditLogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "platform_user_id")
	private UUID platformUserId;

	@Column(name = "action", nullable = false, length = 30)
	private String action;

	@Column(name = "entity_type", nullable = false, length = 80)
	private String entityType;

	@Column(name = "entity_id")
	private UUID entityId;

	@Column(name = "old_value")
	private String oldValue;

	@Column(name = "new_value")
	private String newValue;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(name = "user_agent")
	private String userAgent;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
