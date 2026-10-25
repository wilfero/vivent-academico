package com.corpoviva.vivent.users.repository;

import com.corpoviva.vivent.users.entity.UserEntity;
import com.corpoviva.vivent.users.entity.UserStatus;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

	@EntityGraph(attributePaths = {
		"company",
		"branch",
		"role",
		"role.permissions",
		"permissionOverrides",
		"permissionOverrides.permission"
	})
	@Query("""
		select distinct user
		from UserEntity user
		where user.company.tenantSlug = :tenantSlug
		  and user.username = :username
		  and user.deletedAt is null
		""")
	Optional<UserEntity> findForAuthentication(@Param("tenantSlug") String tenantSlug, @Param("username") String username);

	@EntityGraph(attributePaths = {
		"company",
		"branch",
		"role",
		"role.permissions",
		"permissionOverrides",
		"permissionOverrides.permission"
	})
	@Query("""
		select distinct user
		from UserEntity user
		where user.id = :userId
		  and user.company.id = :companyId
		  and user.deletedAt is null
		""")
	Optional<UserEntity> findForTokenAuthentication(@Param("userId") UUID userId, @Param("companyId") UUID companyId);

	@EntityGraph(attributePaths = {"branch", "role"})
	@Query("""
		select user
		from UserEntity user
		where user.company.id = :companyId
		  and user.deletedAt is null
		  and (:status is null or user.status = :status)
		  and (:branchId is null or user.branch.id = :branchId)
		  and (
		      :query is null
		      or lower(user.username) like lower(concat('%', :query, '%'))
		      or lower(user.firstName) like lower(concat('%', :query, '%'))
		      or lower(user.lastName) like lower(concat('%', :query, '%'))
		      or lower(user.email) like lower(concat('%', :query, '%'))
		  )
		""")
	Page<UserEntity> searchByCompany(
		@Param("companyId") UUID companyId,
		@Param("query") String query,
		@Param("status") UserStatus status,
		@Param("branchId") UUID branchId,
		Pageable pageable
	);

	@EntityGraph(attributePaths = {
		"company",
		"branch",
		"role",
		"role.permissions",
		"permissionOverrides",
		"permissionOverrides.permission"
	})
	Optional<UserEntity> findByIdAndCompany_IdAndDeletedAtIsNull(UUID userId, UUID companyId);

	boolean existsByCompany_IdAndUsernameIgnoreCaseAndDeletedAtIsNull(UUID companyId, String username);

	List<UserEntity> findByCompany_IdAndIdInAndDeletedAtIsNull(UUID companyId, Collection<UUID> userIds);

	@Query("""
		select count(user)
		from UserEntity user
		where user.company.id = :companyId
		  and user.deletedAt is null
		  and user.status = com.corpoviva.vivent.users.entity.UserStatus.ACTIVE
		""")
	long countActiveByCompanyId(@Param("companyId") UUID companyId);

	@EntityGraph(attributePaths = {"role"})
	@Query("""
		select user
		from UserEntity user
		where user.company.id = :companyId
		  and user.role.code = 'COMPANY_ADMIN'
		  and user.deletedAt is null
		order by user.createdAt asc
		""")
	List<UserEntity> findCompanyAdminsByCompanyId(@Param("companyId") UUID companyId);
}
