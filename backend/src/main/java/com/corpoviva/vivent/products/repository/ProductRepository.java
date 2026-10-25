package com.corpoviva.vivent.products.repository;

import com.corpoviva.vivent.products.entity.ProductEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

	@EntityGraph(attributePaths = "category")
	@Query("""
		select product
		from ProductEntity product
		where product.company.id = :companyId
		  and product.deletedAt is null
		  and (:active is null or product.active = :active)
		  and (:categoryId is null or product.category.id = :categoryId)
		  and (
		      lower(product.name) like lower(concat('%', :query, '%'))
		      or lower(coalesce(product.sku, '')) like lower(concat('%', :query, '%'))
		      or lower(product.baseUnitCode) like lower(concat('%', :query, '%'))
		  )
		""")
	Page<ProductEntity> searchByCompany(
		@Param("companyId") UUID companyId,
		@Param("query") String query,
		@Param("active") Boolean active,
		@Param("categoryId") UUID categoryId,
		Pageable pageable
	);

	@EntityGraph(attributePaths = "category")
	Optional<ProductEntity> findByIdAndCompany_IdAndDeletedAtIsNull(UUID productId, UUID companyId);

	boolean existsByCompany_IdAndSkuIgnoreCaseAndDeletedAtIsNull(UUID companyId, String sku);

	@Query("""
		select count(product)
		from ProductEntity product
		where product.company.id = :companyId
		  and product.deletedAt is null
		  and product.active = true
		""")
	long countActiveByCompanyId(@Param("companyId") UUID companyId);
}
