package com.corpoviva.vivent.products.service;

import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.products.dto.ProductUnitResponse;
import com.corpoviva.vivent.products.dto.UpsertProductUnitRequest;
import com.corpoviva.vivent.products.entity.ProductUnitEntity;
import com.corpoviva.vivent.products.repository.ProductUnitRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductUnitService {

	private final ProductUnitRepository productUnitRepository;

	public ProductUnitService(ProductUnitRepository productUnitRepository) {
		this.productUnitRepository = productUnitRepository;
	}

	@Transactional(readOnly = true)
	public List<ProductUnitResponse> listActiveUnits() {
		return productUnitRepository.findByActiveTrueOrderByCodeAsc().stream().map(ProductUnitResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public List<ProductUnitResponse> listUnits(boolean includeInactive) {
		return (includeInactive ? productUnitRepository.findAllByOrderByCodeAsc() : productUnitRepository.findByActiveTrueOrderByCodeAsc())
			.stream()
			.map(ProductUnitResponse::from)
			.toList();
	}

	@Transactional
	public ProductUnitResponse createUnit(UpsertProductUnitRequest request) {
		String code = normalizeCode(request.code());
		if (productUnitRepository.existsById(code)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Product unit code already exists");
		}

		ProductUnitEntity unit = new ProductUnitEntity();
		unit.setCode(code);
		applyUnitFields(unit, request);
		return ProductUnitResponse.from(productUnitRepository.save(unit));
	}

	@Transactional
	public ProductUnitResponse updateUnit(String code, UpsertProductUnitRequest request) {
		String normalizedCode = normalizeCode(code);
		if (!normalizedCode.equals(normalizeCode(request.code()))) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Product unit code cannot be changed");
		}

		ProductUnitEntity unit = productUnitRepository.findById(normalizedCode)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Product unit not found"));
		applyUnitFields(unit, request);
		return ProductUnitResponse.from(unit);
	}

	@Transactional
	public void deactivateUnit(String code) {
		ProductUnitEntity unit = productUnitRepository.findById(normalizeCode(code))
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Product unit not found"));
		unit.setActive(false);
	}

	@Transactional(readOnly = true)
	public ProductUnitEntity loadActiveUnit(String code) {
		return productUnitRepository.findById(normalizeCode(code))
			.filter(ProductUnitEntity::isActive)
			.orElseThrow(() -> ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Invalid product unit code"));
	}

	private void applyUnitFields(ProductUnitEntity unit, UpsertProductUnitRequest request) {
		unit.setName(request.name().trim());
		unit.setTranslationKey(request.translationKey().trim());
		unit.setAllowsDecimal(request.allowsDecimal());
		unit.setActive(request.active());
	}

	private String normalizeCode(String code) {
		return code.trim().toLowerCase(java.util.Locale.ROOT);
	}
}
