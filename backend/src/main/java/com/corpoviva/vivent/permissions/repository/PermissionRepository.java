package com.corpoviva.vivent.permissions.repository;

import com.corpoviva.vivent.permissions.entity.PermissionEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {

	List<PermissionEntity> findAllByOrderByModuleCodeAscActionCodeAsc();

	List<PermissionEntity> findByCodeIn(Collection<String> codes);
}
