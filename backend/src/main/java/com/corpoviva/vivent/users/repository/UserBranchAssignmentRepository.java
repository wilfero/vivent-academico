package com.corpoviva.vivent.users.repository;

import com.corpoviva.vivent.users.entity.UserBranchAssignmentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBranchAssignmentRepository extends JpaRepository<UserBranchAssignmentEntity, UUID> {

	List<UserBranchAssignmentEntity> findByUser_Id(UUID userId);
}
