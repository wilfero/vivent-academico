package com.corpoviva.vivent.combos.repository;

import com.corpoviva.vivent.combos.entity.ComboOfferItemEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComboOfferItemRepository extends JpaRepository<ComboOfferItemEntity, UUID> {

	List<ComboOfferItemEntity> findByComboOffer_Id(UUID comboOfferId);

	@Modifying
	@Query("delete from ComboOfferItemEntity item where item.comboOffer.id = :comboOfferId")
	void deleteByComboOffer_Id(@Param("comboOfferId") UUID comboOfferId);
}
