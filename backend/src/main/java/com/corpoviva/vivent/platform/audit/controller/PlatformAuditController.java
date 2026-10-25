package com.corpoviva.vivent.platform.audit.controller;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.response.ApiResponse;
import com.corpoviva.vivent.platform.audit.dto.PlatformAuditLogResponse;
import com.corpoviva.vivent.platform.audit.service.PlatformAuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform/audit-logs")
public class PlatformAuditController {

	private final PlatformAuditService platformAuditService;

	public PlatformAuditController(PlatformAuditService platformAuditService) {
		this.platformAuditService = platformAuditService;
	}

	@GetMapping
	public ApiResponse<PageResponse<PlatformAuditLogResponse>> listLogs(
		@RequestParam(name = "page", defaultValue = "1") int page,
		@RequestParam(name = "pageSize", defaultValue = "20") int pageSize
	) {
		return ApiResponse.success(platformAuditService.listLogs(page, pageSize));
	}
}
