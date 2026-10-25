package com.corpoviva.vivent.audit.dto;

import com.corpoviva.vivent.audit.entity.AuditAction;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
	UUID id,
	UUID companyId,
	UUID branchId,
	UUID userId,
	AuditAction action,
	String entityType,
	UUID entityId,
	String oldValue,
	String newValue,
	String ipAddress,
	String userAgent,
	String timezoneSnapshot,
	Instant createdAt
) {}
