package com.corpoviva.vivent.combos.service;

import com.corpoviva.vivent.combos.dto.ComboOfferItemRequest;
import com.corpoviva.vivent.combos.dto.ComboOfferResponse;
import com.corpoviva.vivent.combos.dto.UpsertComboOfferRequest;
import com.corpoviva.vivent.combos.entity.ComboOfferEntity;
import com.corpoviva.vivent.combos.entity.ComboOfferItemEntity;
import com.corpoviva.vivent.combos.repository.ComboOfferItemRepository;
import com.corpoviva.vivent.combos.repository.ComboOfferRepository;
import com.corpoviva.vivent.combos.repository.SaleComboLineRepository;
import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.products.entity.ProductEntity;
import com.corpoviva.vivent.products.entity.ProductPresentationEntity;
import com.corpoviva.vivent.products.repository.ProductPresentationRepository;
import com.corpoviva.vivent.products.repository.ProductRepository;
import com.corpoviva.vivent.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComboOfferService {

	private final ComboOfferRepository comboOfferRepository;
	private final ComboOfferItemRepository comboOfferItemRepository;
	private final SaleComboLineRepository saleComboLineRepository;
	private final CompanyRepository companyRepository;
	private final ProductRepository productRepository;
	private final ProductPresentationRepository productPresentationRepository;

	public ComboOfferService(
		ComboOfferRepository comboOfferRepository,
		ComboOfferItemRepository comboOfferItemRepository,
		SaleComboLineRepository saleComboLineRepository,
		CompanyRepository companyRepository,
		ProductRepository productRepository,
		ProductPresentationRepository productPresentationRepository
	) {
		this.comboOfferRepository = comboOfferRepository;
		this.comboOfferItemRepository = comboOfferItemRepository;
		this.saleComboLineRepository = saleComboLineRepository;
		this.companyRepository = companyRepository;
		this.productRepository = productRepository;
		this.productPresentationRepository = productPresentationRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<ComboOfferResponse> listCombos(
		AuthenticatedUser user,
		int page,
		int pageSize,
		Boolean active,
		String query
	) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		String normalizedQuery = trimToNull(query) == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		Page<ComboOfferEntity> comboPage = comboOfferRepository.searchByCompany(
			user.companyId(),
			normalizedQuery,
			active,
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("createdAt").descending())
		);
		List<UUID> comboIds = comboPage.getContent().stream().map(ComboOfferEntity::getId).toList();
		Map<UUID, ComboOfferEntity> combosById = comboIds.isEmpty()
			? Map.of()
			: comboOfferRepository.findAllWithItemsByIdIn(comboIds).stream()
				.collect(Collectors.toMap(ComboOfferEntity::getId, Function.identity()));
		List<ComboOfferResponse> items = comboPage.getContent().stream()
			.map(combo -> ComboOfferResponse.from(combosById.getOrDefault(combo.getId(), combo)))
			.toList();
		return new PageResponse<>(items, normalizedPage, normalizedPageSize, comboPage.getTotalElements(), comboPage.getTotalPages());
	}

	@Transactional(readOnly = true)
	public ComboOfferResponse getCombo(AuthenticatedUser user, UUID comboId) {
		return ComboOfferResponse.from(loadCombo(user.companyId(), comboId));
	}

	@Transactional(readOnly = true)
	public ComboOfferResponse getComboByBarcode(AuthenticatedUser user, String barcode) {
		String trimmed = trimToNull(barcode);
		if (trimmed == null) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Barcode is required");
		}
		return comboOfferRepository.findByCompany_IdAndBarcodeAndDeletedAtIsNull(user.companyId(), trimmed)
			.map(ComboOfferResponse::from)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.BARCODE_NOT_FOUND, "Combo barcode not found"));
	}

	@Transactional
	public ComboOfferResponse createCombo(AuthenticatedUser user, UpsertComboOfferRequest request) {
		CompanyEntity company = loadCompany(user.companyId());
		String code = trimToNull(request.code());
		if (code == null) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo code is required");
		}
		if (comboOfferRepository.existsByCompany_IdAndCodeIgnoreCaseAndDeletedAtIsNull(user.companyId(), code)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo code already exists in the company");
		}
		String barcode = trimToNull(request.barcode());
		if (barcode != null && comboOfferRepository.existsByCompany_IdAndBarcodeAndDeletedAtIsNull(user.companyId(), barcode)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo barcode already exists in the company");
		}
		if (request.items() == null || request.items().isEmpty()) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo must have at least one item");
		}
		validateSchedule(request.startAt(), request.endAt());

		ComboOfferEntity combo = new ComboOfferEntity();
		combo.setCompany(company);
		combo.setName(trimToNull(request.name()));
		combo.setCode(code);
		combo.setBarcode(barcode);
		combo.setDescription(trimToNull(request.description()));
		combo.setComboPrice(request.comboPrice());
		combo.setStartAt(request.startAt());
		combo.setEndAt(request.endAt());
		combo.setActive(request.isActive() == null ? true : request.isActive());
		ComboOfferEntity savedCombo = comboOfferRepository.save(combo);

		saveItems(user.companyId(), savedCombo, request.items(), request.comboPrice());
		return ComboOfferResponse.from(loadCombo(user.companyId(), savedCombo.getId()));
	}

	@Transactional
	public ComboOfferResponse updateCombo(AuthenticatedUser user, UUID comboId, UpsertComboOfferRequest request) {
		ComboOfferEntity combo = loadCombo(user.companyId(), comboId);
		String code = trimToNull(request.code());
		if (code == null) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo code is required");
		}
		if (!code.equalsIgnoreCase(combo.getCode()) && comboOfferRepository.existsByCompany_IdAndCodeIgnoreCaseAndDeletedAtIsNull(user.companyId(), code)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo code already exists in the company");
		}
		String barcode = trimToNull(request.barcode());
		if (barcode != null && !barcode.equals(combo.getBarcode()) && comboOfferRepository.existsByCompany_IdAndBarcodeAndDeletedAtIsNull(user.companyId(), barcode)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo barcode already exists in the company");
		}
		if (request.items() == null || request.items().isEmpty()) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo must have at least one item");
		}
		validateSchedule(request.startAt(), request.endAt());

		combo.setName(trimToNull(request.name()));
		combo.setCode(code);
		combo.setBarcode(barcode);
		combo.setDescription(trimToNull(request.description()));
		combo.setComboPrice(request.comboPrice());
		combo.setStartAt(request.startAt());
		combo.setEndAt(request.endAt());
		if (request.isActive() != null) {
			combo.setActive(request.isActive());
		}
		comboOfferRepository.save(combo);

		// Replace items: delete old and save new explicitly via repository
		comboOfferItemRepository.deleteByComboOffer_Id(combo.getId());
		comboOfferItemRepository.flush();
		saveItems(user.companyId(), combo, request.items(), request.comboPrice());
		return ComboOfferResponse.from(loadCombo(user.companyId(), combo.getId()));
	}

	@Transactional
	public void deleteCombo(AuthenticatedUser user, UUID comboId) {
		ComboOfferEntity combo = loadCombo(user.companyId(), comboId);
		combo.setDeletedAt(Instant.now());
		combo.setActive(false);
		comboOfferRepository.save(combo);
	}

	private void saveItems(UUID companyId, ComboOfferEntity combo, List<ComboOfferItemRequest> itemRequests, BigDecimal comboPrice) {
		List<ComboOfferItemEntity> items = new ArrayList<>();
		BigDecimal regularTotal = BigDecimal.ZERO;
		for (ComboOfferItemRequest itemRequest : itemRequests) {
			ProductEntity product = productRepository.findByIdAndCompany_IdAndDeletedAtIsNull(itemRequest.productId(), companyId)
				.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Product not found"));
			if (!product.isActive() || !product.isStockable()) {
				throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo product must be active and stockable");
			}
			ProductPresentationEntity presentation = productPresentationRepository
				.findByIdAndProduct_IdAndCompany_IdAndDeletedAtIsNull(itemRequest.presentationId(), product.getId(), companyId)
				.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Presentation not found"));
			if (!presentation.isActive()) {
				throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Presentation must be active");
			}

			BigDecimal conversionFactor = presentation.getConversionFactor();
			BigDecimal quantityPresentation = itemRequest.quantityPresentation();
			BigDecimal quantityBase = quantityPresentation.multiply(conversionFactor);

			ComboOfferItemEntity item = new ComboOfferItemEntity();
			item.setComboOffer(combo);
			item.setProduct(product);
			item.setPresentation(presentation);
			item.setQuantityPresentation(quantityPresentation);
			item.setConversionFactor(conversionFactor);
			item.setQuantityBase(quantityBase);
			item.setRegularUnitPriceSnapshot(presentation.getSalePrice());
			items.add(item);
			regularTotal = regularTotal.add(quantityPresentation.multiply(presentation.getSalePrice()));
		}
		if (comboPrice.compareTo(regularTotal) > 0) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo price cannot exceed the regular total of its items");
		}
		comboOfferItemRepository.saveAll(items);
		comboOfferItemRepository.flush();
		combo.getItems().clear();
		combo.getItems().addAll(items);
	}

	private void validateSchedule(Instant startAt, Instant endAt) {
		if (startAt != null && endAt != null && !startAt.isBefore(endAt)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo start date must be before end date");
		}
	}

	private ComboOfferEntity loadCombo(UUID companyId, UUID comboId) {
		return comboOfferRepository.findByIdAndCompany_IdAndDeletedAtIsNull(comboId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Combo not found"));
	}

	private CompanyEntity loadCompany(UUID companyId) {
		return companyRepository.findByIdAndDeletedAtIsNull(companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Company not found"));
	}

	private String trimToNull(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
