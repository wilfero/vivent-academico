package com.corpoviva.vivent.sales.repository;

import com.corpoviva.vivent.sales.entity.SaleDetailEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleDetailRepository extends JpaRepository<SaleDetailEntity, UUID> {

	@EntityGraph(attributePaths = {"product", "presentation"})
	List<SaleDetailEntity> findBySale_IdOrderByCreatedAtAsc(UUID saleId);

	@Modifying
	@Query("""
		delete from SaleDetailEntity detail
		where detail.sale.id in (
			select sale.id
			from SaleEntity sale
			where sale.company.id = :companyId
			  and sale.status = com.corpoviva.vivent.sales.entity.SaleStatus.DRAFT
			  and (:branchId is null or sale.branch.id = :branchId)
		)
		""")
	void deleteDraftDetails(@Param("companyId") UUID companyId, @Param("branchId") UUID branchId);
}
