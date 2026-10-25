package com.corpoviva.vivent.platform.audit.dto;

import com.corpoviva.vivent.platform.entity.PlatformAuditLogEntity;
import java.time.Instant;
import java.util.UUID;

public record PlatformAuditLogResponse(
	UUID id,
	UUID platformUserId,
	String action,
	String entityType,
	UUID entityId,
	String oldValue,
	String newValue,
	String ipAddress,
	String userAgent,
	Instant createdAt
) {

	public static PlatformAuditLogResponse from(PlatformAuditLogEntity log) {
		return new PlatformAuditLogResponse(
			log.getId(),
			log.getPlatformUserId(),
			log.getAction(),
			log.getEntityType(),
			log.getEntityId(),
			log.getOldValue(),
			log.getNewValue(),
			log.getIpAddress(),
			log.getUserAgent(),
			log.getCreatedAt()
		);
	}
}
