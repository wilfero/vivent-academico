package com.corpoviva.vivent.purchases.repository;

import com.corpoviva.vivent.purchases.entity.SupplierEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface SupplierRepository extends JpaRepository<SupplierEntity, UUID>, JpaSpecificationExecutor<SupplierEntity> {

	Page<SupplierEntity> findAll(Specification<SupplierEntity> specification, Pageable pageable);

	Optional<SupplierEntity> findByIdAndCompany_IdAndDeletedAtIsNull(UUID supplierId, UUID companyId);

	boolean existsByCompany_IdAndNameIgnoreCaseAndDeletedAtIsNull(UUID companyId, String name);
}
