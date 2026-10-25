package com.corpoviva.vivent.transfers.repository;

import com.corpoviva.vivent.transfers.entity.TransferDetailEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferDetailRepository extends JpaRepository<TransferDetailEntity, UUID> {

	@EntityGraph(attributePaths = {"product", "presentation"})
	List<TransferDetailEntity> findByTransfer_IdOrderByCreatedAtAsc(UUID transferId);

	void deleteByTransfer_Id(UUID transferId);
}
