package com.corpoviva.vivent.inventory.repository;

import com.corpoviva.vivent.inventory.entity.StockMovementEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovementEntity, UUID>, JpaSpecificationExecutor<StockMovementEntity> {

	@Query("""
		select count(movement)
		from StockMovementEntity movement
		where movement.company.id = :companyId
		  and (:branchId is null or movement.branch.id = :branchId)
		""")
	long countByCompanyIdAndOptionalBranchId(@Param("companyId") UUID companyId, @Param("branchId") UUID branchId);
}
