package com.corpoviva.vivent.products.repository;

import com.corpoviva.vivent.products.entity.CategoryEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

	@Query("""
		select category
		from CategoryEntity category
		where category.company.id = :companyId
		  and category.deletedAt is null
		  and (:active is null or category.active = :active)
		  and (
		      lower(category.name) like lower(concat('%', :query, '%'))
		      or lower(coalesce(category.description, '')) like lower(concat('%', :query, '%'))
		  )
		""")
	Page<CategoryEntity> searchByCompany(
		@Param("companyId") UUID companyId,
		@Param("query") String query,
		@Param("active") Boolean active,
		Pageable pageable
	);

	Optional<CategoryEntity> findByIdAndCompany_IdAndDeletedAtIsNull(UUID categoryId, UUID companyId);

	boolean existsByCompany_IdAndNameIgnoreCaseAndDeletedAtIsNull(UUID companyId, String name);
}
