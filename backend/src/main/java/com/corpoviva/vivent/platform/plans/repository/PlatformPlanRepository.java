package com.corpoviva.vivent.platform.plans.repository;

import com.corpoviva.vivent.subscriptions.entity.PlanEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformPlanRepository extends JpaRepository<PlanEntity, UUID> {

	List<PlanEntity> findAllByActiveTrueOrderByNameAsc();
}
