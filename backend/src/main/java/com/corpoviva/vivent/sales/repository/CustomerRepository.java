package com.corpoviva.vivent.sales.repository;

import com.corpoviva.vivent.sales.entity.CustomerEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID>, JpaSpecificationExecutor<CustomerEntity> {

	Optional<CustomerEntity> findByIdAndCompany_IdAndDeletedAtIsNull(UUID customerId, UUID companyId);

	Optional<CustomerEntity> findFirstByCompany_IdAndGenericConsumerTrueAndDeletedAtIsNull(UUID companyId);
}
