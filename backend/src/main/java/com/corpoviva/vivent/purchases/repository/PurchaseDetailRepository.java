package com.corpoviva.vivent.purchases.repository;

import com.corpoviva.vivent.purchases.entity.PurchaseDetailEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseDetailRepository extends JpaRepository<PurchaseDetailEntity, UUID> {

	@EntityGraph(attributePaths = {"product", "presentation"})
	List<PurchaseDetailEntity> findByPurchase_IdOrderByCreatedAtAsc(UUID purchaseId);

	void deleteByPurchase_Id(UUID purchaseId);
}
