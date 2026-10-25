package com.corpoviva.vivent.subscriptions.repository;

import com.corpoviva.vivent.subscriptions.entity.SubscriptionEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {

	@EntityGraph(attributePaths = "plan")
	Optional<SubscriptionEntity> findFirstByCompanyIdOrderByCreatedAtDesc(UUID companyId);

	@EntityGraph(attributePaths = "plan")
	@Query("""
		select subscription
		from SubscriptionEntity subscription
		where subscription.companyId in :companyIds
		order by subscription.createdAt desc
		""")
	List<SubscriptionEntity> findLatestByCompanyIdIn(@Param("companyIds") Collection<UUID> companyIds);
}
