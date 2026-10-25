package com.corpoviva.vivent.audit.controller;

import com.corpoviva.vivent.audit.dto.AuditLogResponse;
import com.corpoviva.vivent.audit.service.AuditLogService;
import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.security.AuthenticatedUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

	private final AuditLogService auditLogService;

	public AuditLogController(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@GetMapping
	@PreAuthorize("hasAuthority('settings.view')")
	public ApiResponse<PageResponse<AuditLogResponse>> listLogs(
		@AuthenticationPrincipal AuthenticatedUser user,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
		@RequestParam(required = false) String entityType,
		@RequestParam(required = false) UUID entityId
	) {
		return ApiResponse.success(auditLogService.listLogs(user, page, pageSize, entityType, entityId));
	}
}
