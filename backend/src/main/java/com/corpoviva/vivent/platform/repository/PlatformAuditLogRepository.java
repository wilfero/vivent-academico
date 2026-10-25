package com.corpoviva.vivent.platform.repository;

import com.corpoviva.vivent.platform.entity.PlatformAuditLogEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAuditLogRepository extends JpaRepository<PlatformAuditLogEntity, UUID> {

	Page<PlatformAuditLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
