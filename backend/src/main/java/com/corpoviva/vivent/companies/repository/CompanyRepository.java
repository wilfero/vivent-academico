package com.corpoviva.vivent.companies.repository;

import com.corpoviva.vivent.companies.entity.CompanyEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CompanyRepository extends JpaRepository<CompanyEntity, UUID>, JpaSpecificationExecutor<CompanyEntity> {

	Optional<CompanyEntity> findByIdAndDeletedAtIsNull(UUID companyId);

	Optional<CompanyEntity> findByTenantSlugAndDeletedAtIsNull(String tenantSlug);

	boolean existsByTenantSlug(String tenantSlug);

	boolean existsByDomainIgnoreCase(String domain);
}
