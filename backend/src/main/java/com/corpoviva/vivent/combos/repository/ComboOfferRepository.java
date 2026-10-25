package com.corpoviva.vivent.combos.repository;

import com.corpoviva.vivent.combos.entity.ComboOfferEntity;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComboOfferRepository extends JpaRepository<ComboOfferEntity, UUID> {

	@Query("""
		select combo
		from ComboOfferEntity combo
		where combo.company.id = :companyId
		  and combo.deletedAt is null
		  and (:active is null or combo.active = :active)
		  and (
		    :query = ''
		    or lower(combo.name) like lower(concat('%', :query, '%'))
		    or lower(combo.code) like lower(concat('%', :query, '%'))
		    or lower(coalesce(combo.barcode, '')) like lower(concat('%', :query, '%'))
		  )
		""")
	Page<ComboOfferEntity> searchByCompany(
		@Param("companyId") UUID companyId,
		@Param("query") String query,
		@Param("active") Boolean active,
		Pageable pageable
	);

	@EntityGraph(attributePaths = {"items", "items.product", "items.presentation"})
	@Query("select distinct combo from ComboOfferEntity combo where combo.id in :comboIds")
	List<ComboOfferEntity> findAllWithItemsByIdIn(@Param("comboIds") Collection<UUID> comboIds);

	@EntityGraph(attributePaths = {"items", "items.product", "items.presentation"})
	Optional<ComboOfferEntity> findByIdAndCompany_IdAndDeletedAtIsNull(UUID comboId, UUID companyId);

	@EntityGraph(attributePaths = {"items", "items.product", "items.presentation"})
	Optional<ComboOfferEntity> findByCompany_IdAndBarcodeAndDeletedAtIsNull(UUID companyId, String barcode);

	boolean existsByCompany_IdAndCodeIgnoreCaseAndDeletedAtIsNull(UUID companyId, String code);

	boolean existsByCompany_IdAndBarcodeAndDeletedAtIsNull(UUID companyId, String barcode);
}
