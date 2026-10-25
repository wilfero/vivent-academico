package com.corpoviva.vivent.sales.repository;

import com.corpoviva.vivent.sales.entity.SaleEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<SaleEntity, UUID>, JpaSpecificationExecutor<SaleEntity> {

	@EntityGraph(attributePaths = {"branch", "customer"})
	Page<SaleEntity> findAll(Specification<SaleEntity> specification, Pageable pageable);

	@EntityGraph(attributePaths = {"company", "branch", "customer"})
	Optional<SaleEntity> findByIdAndCompany_Id(UUID saleId, UUID companyId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"company", "branch", "customer"})
	@Query("""
		select sale
		from SaleEntity sale
		where sale.id = :saleId
		  and sale.company.id = :companyId
		""")
	Optional<SaleEntity> findByIdAndCompanyIdForUpdate(@Param("saleId") UUID saleId, @Param("companyId") UUID companyId);

	boolean existsByCompany_IdAndSaleNumberIgnoreCase(UUID companyId, String saleNumber);

	@Query("""
		select count(sale)
		from SaleEntity sale
		where sale.company.id = :companyId
		  and sale.status = com.corpoviva.vivent.sales.entity.SaleStatus.DRAFT
		  and (:branchId is null or sale.branch.id = :branchId)
		""")
	long countDrafts(@Param("companyId") UUID companyId, @Param("branchId") UUID branchId);

	@Modifying
	@Query("""
		delete from SaleEntity sale
		where sale.company.id = :companyId
		  and sale.status = com.corpoviva.vivent.sales.entity.SaleStatus.DRAFT
		  and (:branchId is null or sale.branch.id = :branchId)
		""")
	void deleteDrafts(@Param("companyId") UUID companyId, @Param("branchId") UUID branchId);
}
