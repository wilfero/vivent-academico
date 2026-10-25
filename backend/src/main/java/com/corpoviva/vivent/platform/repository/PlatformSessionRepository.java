package com.corpoviva.vivent.platform.repository;

import com.corpoviva.vivent.platform.entity.PlatformSessionEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformSessionRepository extends JpaRepository<PlatformSessionEntity, UUID> {

	Optional<PlatformSessionEntity> findByTokenHash(String tokenHash);

	@Modifying
	@Query("delete from PlatformSessionEntity session where session.revokedAt is not null and session.createdAt < :cutoff")
	int deleteRevokedOlderThan(@Param("cutoff") Instant cutoff);

	@Modifying
	@Query("delete from PlatformSessionEntity session where session.expiresAt < :now")
	int deleteExpired(@Param("now") Instant now);
}
