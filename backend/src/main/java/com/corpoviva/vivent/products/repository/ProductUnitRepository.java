package com.corpoviva.vivent.products.repository;

import com.corpoviva.vivent.products.entity.ProductUnitEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductUnitRepository extends JpaRepository<ProductUnitEntity, String> {

	List<ProductUnitEntity> findByActiveTrueOrderByCodeAsc();

	List<ProductUnitEntity> findAllByOrderByCodeAsc();
}
