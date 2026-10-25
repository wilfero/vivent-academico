package com.corpoviva.vivent.combos.repository;

import com.corpoviva.vivent.combos.entity.SaleComboLineEntity;
import java.util.List;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleComboLineRepository extends JpaRepository<SaleComboLineEntity, UUID> {

	@EntityGraph(attributePaths = {"comboOffer"})
	List<SaleComboLineEntity> findBySale_IdOrderByCreatedAtAsc(UUID saleId);

	@EntityGraph(attributePaths = {"sale", "comboOffer"})
	List<SaleComboLineEntity> findBySale_IdInOrderByCreatedAtAsc(Collection<UUID> saleIds);

	@Modifying
	@Query("""
		delete from SaleComboLineEntity comboLine
		where comboLine.sale.id in (
			select sale.id
			from SaleEntity sale
			where sale.company.id = :companyId
			  and sale.status = com.corpoviva.vivent.sales.entity.SaleStatus.DRAFT
			  and (:branchId is null or sale.branch.id = :branchId)
		)
		""")
	void deleteDraftComboLines(@Param("companyId") UUID companyId, @Param("branchId") UUID branchId);
}
