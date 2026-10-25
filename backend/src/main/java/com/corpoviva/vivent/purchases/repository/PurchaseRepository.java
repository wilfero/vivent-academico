package com.corpoviva.vivent.purchases.repository;

import com.corpoviva.vivent.purchases.entity.PurchaseEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRepository extends JpaRepository<PurchaseEntity, UUID>, JpaSpecificationExecutor<PurchaseEntity> {

	@EntityGraph(attributePaths = {"branch", "supplier"})
	org.springframework.data.domain.Page<PurchaseEntity> findAll(
		org.springframework.data.jpa.domain.Specification<PurchaseEntity> specification,
		org.springframework.data.domain.Pageable pageable
	);

	@EntityGraph(attributePaths = {"company", "branch", "supplier"})
	Optional<PurchaseEntity> findByIdAndCompany_Id(UUID purchaseId, UUID companyId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"company", "branch", "supplier"})
	@Query("""
		select purchase
		from PurchaseEntity purchase
		where purchase.id = :purchaseId
		  and purchase.company.id = :companyId
		""")
	Optional<PurchaseEntity> findByIdAndCompanyIdForUpdate(@Param("purchaseId") UUID purchaseId, @Param("companyId") UUID companyId);

	boolean existsByCompany_IdAndPurchaseNumberIgnoreCase(UUID companyId, String purchaseNumber);
}
