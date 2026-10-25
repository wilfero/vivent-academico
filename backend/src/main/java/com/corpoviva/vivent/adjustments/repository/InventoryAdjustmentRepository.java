package com.corpoviva.vivent.adjustments.repository;

import com.corpoviva.vivent.adjustments.entity.AdjustmentStatus;
import com.corpoviva.vivent.adjustments.entity.InventoryAdjustmentEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustmentEntity, UUID>, JpaSpecificationExecutor<InventoryAdjustmentEntity> {

	@EntityGraph(attributePaths = {"branch", "location"})
	Page<InventoryAdjustmentEntity> findAll(org.springframework.data.jpa.domain.Specification<InventoryAdjustmentEntity> specification, Pageable pageable);

	@EntityGraph(attributePaths = {"branch", "location"})
	@Query("""
		select adjustment
		from InventoryAdjustmentEntity adjustment
		where adjustment.company.id = :companyId
		  and (:branchId is null or adjustment.branch.id = :branchId)
		  and (:status is null or adjustment.status = :status)
		  and (:query = '' or lower(adjustment.adjustmentNumber) like lower(concat('%', :query, '%')))
		""")
	Page<InventoryAdjustmentEntity> searchByCompany(
		@Param("companyId") UUID companyId,
		@Param("branchId") UUID branchId,
		@Param("status") AdjustmentStatus status,
		@Param("query") String query,
		Pageable pageable
	);

	@EntityGraph(attributePaths = {"company", "branch", "location"})
	Optional<InventoryAdjustmentEntity> findByIdAndCompany_Id(UUID adjustmentId, UUID companyId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"company", "branch", "location"})
	@Query("""
		select adjustment
		from InventoryAdjustmentEntity adjustment
		where adjustment.id = :adjustmentId
		  and adjustment.company.id = :companyId
		""")
	Optional<InventoryAdjustmentEntity> findByIdAndCompanyIdForUpdate(@Param("adjustmentId") UUID adjustmentId, @Param("companyId") UUID companyId);

	boolean existsByCompany_IdAndAdjustmentNumberIgnoreCase(UUID companyId, String adjustmentNumber);
}
