package com.corpoviva.vivent.adjustments.repository;

import com.corpoviva.vivent.adjustments.entity.InventoryAdjustmentLineEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentLineRepository extends JpaRepository<InventoryAdjustmentLineEntity, UUID> {

	@EntityGraph(attributePaths = {"product", "presentation"})
	List<InventoryAdjustmentLineEntity> findByAdjustment_IdOrderByCreatedAtAsc(UUID adjustmentId);

	void deleteByAdjustment_Id(UUID adjustmentId);
}
