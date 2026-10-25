package com.corpoviva.vivent.branches.repository;

import com.corpoviva.vivent.branches.entity.LocationEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {

	Optional<LocationEntity> findByIdAndCompany_IdAndBranch_Id(UUID locationId, UUID companyId, UUID branchId);

	Optional<LocationEntity> findByCompany_IdAndBranch_IdAndDefaultLocationTrueAndActiveTrue(UUID companyId, UUID branchId);
}
