package com.corpoviva.vivent.platform.audit.service;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.platform.audit.dto.PlatformAuditLogResponse;
import com.corpoviva.vivent.platform.entity.PlatformAuditLogEntity;
import com.corpoviva.vivent.platform.repository.PlatformAuditLogRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformAuditService {

	private final PlatformAuditLogRepository platformAuditLogRepository;

	public PlatformAuditService(PlatformAuditLogRepository platformAuditLogRepository) {
		this.platformAuditLogRepository = platformAuditLogRepository;
	}

	@Transactional
	public void log(
		UUID platformUserId,
		String action,
		String entityType,
		UUID entityId,
		String oldValue,
		String newValue,
		String ipAddress,
		String userAgent
	) {
		PlatformAuditLogEntity log = new PlatformAuditLogEntity();
		log.setPlatformUserId(platformUserId);
		log.setAction(action);
		log.setEntityType(entityType);
		log.setEntityId(entityId);
		log.setOldValue(oldValue);
		log.setNewValue(newValue);
		log.setIpAddress(ipAddress);
		log.setUserAgent(userAgent);
		platformAuditLogRepository.save(log);
	}

	@Transactional(readOnly = true)
	public PageResponse<PlatformAuditLogResponse> listLogs(int page, int pageSize) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);

		Page<PlatformAuditLogEntity> logPage = platformAuditLogRepository.findAllByOrderByCreatedAtDesc(
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("createdAt").descending())
		);

		Page<PlatformAuditLogResponse> responsePage = logPage.map(PlatformAuditLogResponse::from);
		return PageResponse.from(responsePage, normalizedPage, normalizedPageSize);
	}
}
