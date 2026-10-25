package com.corpoviva.vivent.platform.repository;

import com.corpoviva.vivent.platform.entity.PlatformUserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformUserRepository extends JpaRepository<PlatformUserEntity, UUID> {

	Optional<PlatformUserEntity> findByUsername(String username);

	boolean existsByUsername(String username);
}
