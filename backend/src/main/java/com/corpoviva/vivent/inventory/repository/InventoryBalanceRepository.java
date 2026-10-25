package com.corpoviva.vivent.inventory.repository;

import com.corpoviva.vivent.inventory.entity.InventoryBalanceEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalanceEntity, UUID>, JpaSpecificationExecutor<InventoryBalanceEntity> {

	@Query("""
		select count(balance)
		from InventoryBalanceEntity balance
		where balance.company.id = :companyId
		  and (:branchId is null or balance.branch.id = :branchId)
		  and balance.quantityOnHand > balance.quantityReserved
		""")
	long countProductsWithStock(@Param("companyId") UUID companyId, @Param("branchId") UUID branchId);

	@Query("""
		select count(balance)
		from InventoryBalanceEntity balance
		where balance.company.id = :companyId
		  and (:branchId is null or balance.branch.id = :branchId)
		  and balance.quantityOnHand <= balance.quantityReserved
		""")
	long countProductsWithoutStock(@Param("companyId") UUID companyId, @Param("branchId") UUID branchId);

	@Query("""
		select count(balance)
		from InventoryBalanceEntity balance
		where balance.company.id = :companyId
		  and (:branchId is null or balance.branch.id = :branchId)
		  and balance.product.minimumStock > 0
		  and balance.quantityAvailable > 0
		  and balance.quantityAvailable <= balance.product.minimumStock
		""")
	long countProductsWithLowStock(@Param("companyId") UUID companyId, @Param("branchId") UUID branchId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<InventoryBalanceEntity> findByCompany_IdAndBranch_IdAndLocation_IdAndProduct_Id(
		UUID companyId,
		UUID branchId,
		UUID locationId,
		UUID productId
	);
}
