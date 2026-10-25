package com.corpoviva.vivent.products.repository;

import com.corpoviva.vivent.products.entity.ProductPresentationEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductPresentationRepository extends JpaRepository<ProductPresentationEntity, UUID> {

	List<ProductPresentationEntity> findByCompany_IdAndProduct_IdAndDeletedAtIsNullOrderByNameAsc(UUID companyId, UUID productId);

	@Query(value = """
		select presentation
		from ProductPresentationEntity presentation
		join fetch presentation.product product
		left join fetch product.category
		where presentation.company.id = :companyId
		  and presentation.deletedAt is null
		  and (:productId is null or product.id = :productId)
		  and (:active is null or presentation.active = :active)
		  and (
		    :query = ''
		    or lower(presentation.name) like lower(concat('%', :query, '%'))
		    or lower(coalesce(presentation.barcode, '')) like lower(concat('%', :query, '%'))
		    or lower(product.name) like lower(concat('%', :query, '%'))
		  )
		""",
		countQuery = """
		select count(presentation)
		from ProductPresentationEntity presentation
		join presentation.product product
		where presentation.company.id = :companyId
		  and presentation.deletedAt is null
		  and (:productId is null or product.id = :productId)
		  and (:active is null or presentation.active = :active)
		  and (
		    :query = ''
		    or lower(presentation.name) like lower(concat('%', :query, '%'))
		    or lower(coalesce(presentation.barcode, '')) like lower(concat('%', :query, '%'))
		    or lower(product.name) like lower(concat('%', :query, '%'))
		  )
		""")
	Page<ProductPresentationEntity> searchByCompany(
		@Param("companyId") UUID companyId,
		@Param("query") String query,
		@Param("active") Boolean active,
		@Param("productId") UUID productId,
		Pageable pageable
	);

	@EntityGraph(attributePaths = {"product", "product.category"})
	Optional<ProductPresentationEntity> findByIdAndProduct_IdAndCompany_IdAndDeletedAtIsNull(UUID presentationId, UUID productId, UUID companyId);

	@EntityGraph(attributePaths = {"product", "product.category"})
	Optional<ProductPresentationEntity> findByIdAndCompany_IdAndDeletedAtIsNull(UUID presentationId, UUID companyId);

	@EntityGraph(attributePaths = {"product", "product.category"})
	Optional<ProductPresentationEntity> findByCompany_IdAndBarcodeAndDeletedAtIsNull(UUID companyId, String barcode);

	boolean existsByCompany_IdAndBarcodeAndDeletedAtIsNull(UUID companyId, String barcode);

	boolean existsByProduct_IdAndNameIgnoreCaseAndDeletedAtIsNull(UUID productId, String name);

	@Modifying
	@Query("""
		update ProductPresentationEntity presentation
		set presentation.defaultPurchase = false
		where presentation.product.id = :productId
		  and presentation.company.id = :companyId
		  and presentation.deletedAt is null
		""")
	void clearDefaultPurchase(@Param("companyId") UUID companyId, @Param("productId") UUID productId);

	@Modifying
	@Query("""
		update ProductPresentationEntity presentation
		set presentation.defaultSale = false
		where presentation.product.id = :productId
		  and presentation.company.id = :companyId
		  and presentation.deletedAt is null
		""")
	void clearDefaultSale(@Param("companyId") UUID companyId, @Param("productId") UUID productId);
}
