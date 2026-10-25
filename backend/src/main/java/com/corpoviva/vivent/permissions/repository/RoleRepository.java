package com.corpoviva.vivent.permissions.repository;

import com.corpoviva.vivent.permissions.entity.RoleEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

	@Query("""
		select role
		from RoleEntity role
		where (role.companyId = :companyId or role.companyId is null)
		  and (
		      :query is null
		      or lower(role.code) like lower(concat('%', :query, '%'))
		      or lower(role.name) like lower(concat('%', :query, '%'))
		  )
		""")
	Page<RoleEntity> searchAccessibleRoles(@Param("companyId") UUID companyId, @Param("query") String query, Pageable pageable);

	@EntityGraph(attributePaths = "permissions")
	@Query("""
		select role
		from RoleEntity role
		where role.id = :roleId
		  and (role.companyId = :companyId or role.companyId is null)
		""")
	Optional<RoleEntity> findAccessibleById(@Param("roleId") UUID roleId, @Param("companyId") UUID companyId);

	@Query("""
		select case when count(role) > 0 then true else false end
		from RoleEntity role
		where lower(role.code) = lower(:code)
		  and (role.companyId = :companyId or role.companyId is null)
		  and (:excludedRoleId is null or role.id <> :excludedRoleId)
		""")
	boolean existsAccessibleRoleCode(
		@Param("companyId") UUID companyId,
		@Param("code") String code,
		@Param("excludedRoleId") UUID excludedRoleId
	);

	Optional<RoleEntity> findByCodeAndCompanyIdIsNull(String code);
}
