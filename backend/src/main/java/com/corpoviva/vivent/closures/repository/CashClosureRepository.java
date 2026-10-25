package com.corpoviva.vivent.closures.repository;

import com.corpoviva.vivent.closures.entity.CashClosureEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashClosureRepository extends JpaRepository<CashClosureEntity, UUID>, JpaSpecificationExecutor<CashClosureEntity> {

	@EntityGraph(attributePaths = {"branch", "user"})
	Page<CashClosureEntity> findAll(Specification<CashClosureEntity> specification, Pageable pageable);

	@EntityGraph(attributePaths = {"company", "branch", "user"})
	Optional<CashClosureEntity> findByIdAndCompany_Id(UUID id, UUID companyId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"company", "branch", "user"})
	@Query("""
		select closure
		from CashClosureEntity closure
		where closure.id = :closureId
		  and closure.company.id = :companyId
		""")
	Optional<CashClosureEntity> findByIdAndCompanyIdForUpdate(@Param("closureId") UUID closureId, @Param("companyId") UUID companyId);

	boolean existsByCompany_IdAndBranch_IdAndUser_IdAndClosureDate(UUID companyId, UUID branchId, UUID userId, LocalDate closureDate);
}
