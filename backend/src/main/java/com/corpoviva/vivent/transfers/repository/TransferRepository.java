package com.corpoviva.vivent.transfers.repository;

import com.corpoviva.vivent.transfers.entity.TransferEntity;
import com.corpoviva.vivent.transfers.entity.TransferStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<TransferEntity, UUID>, JpaSpecificationExecutor<TransferEntity> {

	@EntityGraph(attributePaths = {"sourceBranch", "destinationBranch"})
	Page<TransferEntity> findAll(org.springframework.data.jpa.domain.Specification<TransferEntity> specification, Pageable pageable);

	@EntityGraph(attributePaths = {"sourceBranch", "destinationBranch"})
	@Query("""
		select transfer
		from TransferEntity transfer
		where transfer.company.id = :companyId
		  and (:sourceBranchId is null or transfer.sourceBranch.id = :sourceBranchId)
		  and (:destinationBranchId is null or transfer.destinationBranch.id = :destinationBranchId)
		  and (:status is null or transfer.status = :status)
		  and (:query = '' or lower(transfer.transferNumber) like lower(concat('%', :query, '%')))
		""")
	Page<TransferEntity> searchByCompany(
		@Param("companyId") UUID companyId,
		@Param("sourceBranchId") UUID sourceBranchId,
		@Param("destinationBranchId") UUID destinationBranchId,
		@Param("status") TransferStatus status,
		@Param("query") String query,
		Pageable pageable
	);

	@EntityGraph(attributePaths = {"sourceBranch", "destinationBranch"})
	@Query("""
		select transfer
		from TransferEntity transfer
		where transfer.company.id = :companyId
		  and (:sourceBranchId is null or transfer.sourceBranch.id = :sourceBranchId)
		  and (:destinationBranchId is null or transfer.destinationBranch.id = :destinationBranchId)
		  and (:status is null or transfer.status = :status)
		  and (:query = '' or lower(transfer.transferNumber) like lower(concat('%', :query, '%')))
		  and (
		      :visibleBranchId is null
		      or transfer.sourceBranch.id = :visibleBranchId
		      or transfer.destinationBranch.id = :visibleBranchId
		  )
		""")
	Page<TransferEntity> searchVisibleByCompany(
		@Param("companyId") UUID companyId,
		@Param("sourceBranchId") UUID sourceBranchId,
		@Param("destinationBranchId") UUID destinationBranchId,
		@Param("status") TransferStatus status,
		@Param("query") String query,
		@Param("visibleBranchId") UUID visibleBranchId,
		Pageable pageable
	);

	@EntityGraph(attributePaths = {"company", "sourceBranch", "destinationBranch"})
	Optional<TransferEntity> findByIdAndCompany_Id(UUID transferId, UUID companyId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"company", "sourceBranch", "destinationBranch"})
	@Query("""
		select transfer
		from TransferEntity transfer
		where transfer.id = :transferId
		  and transfer.company.id = :companyId
		""")
	Optional<TransferEntity> findByIdAndCompanyIdForUpdate(@Param("transferId") UUID transferId, @Param("companyId") UUID companyId);

	boolean existsByCompany_IdAndTransferNumberIgnoreCase(UUID companyId, String transferNumber);
}
