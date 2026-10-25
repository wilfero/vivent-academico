package com.corpoviva.vivent.inventory.repository;

import com.corpoviva.vivent.inventory.entity.InventoryLayerEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryLayerRepository extends JpaRepository<InventoryLayerEntity, UUID>, JpaSpecificationExecutor<InventoryLayerEntity> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select layer
		from InventoryLayerEntity layer
		where layer.company.id = :companyId
		  and layer.branch.id = :branchId
		  and layer.location.id = :locationId
		  and layer.product.id = :productId
		  and layer.status = com.corpoviva.vivent.inventory.entity.InventoryLayerStatus.OPEN
		  and layer.quantityRemaining > 0
		order by layer.receivedAt asc, layer.id asc
		""")
	List<InventoryLayerEntity> findOpenLayersForUpdate(
		@Param("companyId") UUID companyId,
		@Param("branchId") UUID branchId,
		@Param("locationId") UUID locationId,
		@Param("productId") UUID productId
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select layer
		from InventoryLayerEntity layer
		where layer.company.id = :companyId
		  and layer.branch.id = :branchId
		  and layer.location.id = :locationId
		  and layer.product.id = :productId
		  and layer.purchaseDetailId = :purchaseDetailId
		order by layer.receivedAt asc, layer.id asc
		""")
	List<InventoryLayerEntity> findPurchaseDetailLayersForUpdate(
		@Param("companyId") UUID companyId,
		@Param("branchId") UUID branchId,
		@Param("locationId") UUID locationId,
		@Param("productId") UUID productId,
		@Param("purchaseDetailId") UUID purchaseDetailId
	);
}
