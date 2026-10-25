package com.corpoviva.vivent.branches.repository;

import com.corpoviva.vivent.branches.entity.BranchEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchRepository extends JpaRepository<BranchEntity, UUID>, JpaSpecificationExecutor<BranchEntity> {

	@Query("""
		select branch
		from BranchEntity branch
		where branch.company.id = :companyId
		  and branch.deletedAt is null
		  and (:active is null or branch.active = :active)
		  and (
		      :query is null
		      or lower(branch.name) like lower(concat('%', :query, '%'))
		      or lower(branch.code) like lower(concat('%', :query, '%'))
		  )
		""")
	Page<BranchEntity> searchByCompany(
		@Param("companyId") UUID companyId,
		@Param("query") String query,
		@Param("active") Boolean active,
		Pageable pageable
	);

	Page<BranchEntity> findAll(org.springframework.data.jpa.domain.Specification<BranchEntity> specification, Pageable pageable);

	Optional<BranchEntity> findByIdAndCompany_IdAndDeletedAtIsNull(UUID branchId, UUID companyId);

	boolean existsByCompany_IdAndCodeIgnoreCaseAndDeletedAtIsNull(UUID companyId, String code);

	@Query("""
		select count(branch)
		from BranchEntity branch
		where branch.company.id = :companyId
		  and branch.deletedAt is null
		  and branch.active = true
		""")
	long countActiveByCompanyId(@Param("companyId") UUID companyId);
}
