package com.corpoviva.vivent.products.service;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.products.dto.BarcodeLookupResponse;
import com.corpoviva.vivent.products.dto.ProductPresentationResponse;
import com.corpoviva.vivent.products.dto.UpsertProductPresentationRequest;
import com.corpoviva.vivent.products.entity.ProductEntity;
import com.corpoviva.vivent.products.entity.ProductPresentationEntity;
import com.corpoviva.vivent.products.repository.ProductPresentationRepository;
import com.corpoviva.vivent.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductPresentationService {

	private final ProductPresentationRepository productPresentationRepository;
	private final ProductService productService;

	public ProductPresentationService(ProductPresentationRepository productPresentationRepository, ProductService productService) {
		this.productPresentationRepository = productPresentationRepository;
		this.productService = productService;
	}

	@Transactional(readOnly = true)
	public PageResponse<ProductPresentationResponse> listPresentations(
		AuthenticatedUser user,
		int page,
		int pageSize,
		String query,
		Boolean active,
		UUID productId
	) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		Page<ProductPresentationEntity> presentationPage = productPresentationRepository.searchByCompany(
			user.companyId(),
			normalizeQuery(query),
			active,
			productId,
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("name").ascending())
		);

		return PageResponse.from(presentationPage.map(ProductPresentationResponse::from), normalizedPage, normalizedPageSize);
	}

	@Transactional(readOnly = true)
	public List<ProductPresentationResponse> listPresentations(AuthenticatedUser user, UUID productId) {
		productService.loadProduct(user.companyId(), productId);
		return productPresentationRepository.findByCompany_IdAndProduct_IdAndDeletedAtIsNullOrderByNameAsc(user.companyId(), productId)
			.stream()
			.map(ProductPresentationResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public ProductPresentationResponse getPresentation(AuthenticatedUser user, UUID productId, UUID presentationId) {
		return ProductPresentationResponse.from(loadPresentation(user.companyId(), productId, presentationId));
	}

	@Transactional
	public ProductPresentationResponse createPresentation(
		AuthenticatedUser user,
		UUID productId,
		UpsertProductPresentationRequest request
	) {
		ProductEntity product = productService.loadProduct(user.companyId(), productId);
		validatePresentationNameAvailable(productId, request.name(), null);
		validateBarcodeAvailable(user.companyId(), request.barcode(), null);
		validateConversionFactor(product, request.conversionFactor());

		ProductPresentationEntity presentation = new ProductPresentationEntity();
		presentation.setCompany(product.getCompany());
		presentation.setProduct(product);
		applyPresentationFields(presentation, request);
		clearDefaultFlagsIfNeeded(user.companyId(), productId, request);

		return ProductPresentationResponse.from(productPresentationRepository.save(presentation));
	}

	@Transactional
	public ProductPresentationResponse updatePresentation(
		AuthenticatedUser user,
		UUID productId,
		UUID presentationId,
		UpsertProductPresentationRequest request
	) {
		ProductPresentationEntity presentation = loadPresentation(user.companyId(), productId, presentationId);

		if (!presentation.getName().equalsIgnoreCase(request.name().trim())) {
			validatePresentationNameAvailable(productId, request.name(), presentationId);
		}

		String newBarcode = normalizeBarcode(request.barcode());
		if (newBarcode != null && !newBarcode.equals(presentation.getBarcode())) {
			validateBarcodeAvailable(user.companyId(), newBarcode, presentationId);
		}

		validateConversionFactor(presentation.getProduct(), request.conversionFactor());
		clearDefaultFlagsIfNeeded(user.companyId(), productId, request);
		applyPresentationFields(presentation, request);

		return ProductPresentationResponse.from(presentation);
	}

	@Transactional
	public void deletePresentation(AuthenticatedUser user, UUID productId, UUID presentationId) {
		ProductPresentationEntity presentation = loadPresentation(user.companyId(), productId, presentationId);
		presentation.setActive(false);
		presentation.setDeletedAt(Instant.now());
	}

	@Transactional(readOnly = true)
	public BarcodeLookupResponse lookupBarcode(AuthenticatedUser user, String barcode) {
		String normalizedBarcode = normalizeBarcode(barcode);
		if (normalizedBarcode == null) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Barcode is required");
		}

		ProductPresentationEntity presentation = productPresentationRepository
			.findByCompany_IdAndBarcodeAndDeletedAtIsNull(user.companyId(), normalizedBarcode)
			.filter(ProductPresentationEntity::isActive)
			.filter(value -> value.getProduct().isActive() && value.getProduct().getDeletedAt() == null)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.BARCODE_NOT_FOUND, "Barcode not found"));

		return BarcodeLookupResponse.from(presentation);
	}

	private ProductPresentationEntity loadPresentation(UUID companyId, UUID productId, UUID presentationId) {
		return productPresentationRepository
			.findByIdAndProduct_IdAndCompany_IdAndDeletedAtIsNull(presentationId, productId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Product presentation not found"));
	}

	private void applyPresentationFields(ProductPresentationEntity presentation, UpsertProductPresentationRequest request) {
		presentation.setName(request.name().trim());
		presentation.setBarcode(normalizeBarcode(request.barcode()));
		presentation.setConversionFactor(request.conversionFactor());
		presentation.setPurchasePriceSuggested(request.purchasePriceSuggested());
		presentation.setSalePrice(request.salePrice());
		presentation.setDefaultPurchase(request.defaultPurchase());
		presentation.setDefaultSale(request.defaultSale());
		presentation.setActive(request.active());
	}

	private void clearDefaultFlagsIfNeeded(UUID companyId, UUID productId, UpsertProductPresentationRequest request) {
		if (request.defaultPurchase()) {
			productPresentationRepository.clearDefaultPurchase(companyId, productId);
		}

		if (request.defaultSale()) {
			productPresentationRepository.clearDefaultSale(companyId, productId);
		}
	}

	private void validatePresentationNameAvailable(UUID productId, String name, UUID ignoredPresentationId) {
		if (productPresentationRepository.existsByProduct_IdAndNameIgnoreCaseAndDeletedAtIsNull(productId, name.trim())) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Presentation name already exists for product");
		}
	}

	private void validateBarcodeAvailable(UUID companyId, String barcode, UUID ignoredPresentationId) {
		String normalizedBarcode = normalizeBarcode(barcode);
		if (normalizedBarcode != null && productPresentationRepository.existsByCompany_IdAndBarcodeAndDeletedAtIsNull(companyId, normalizedBarcode)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Barcode already exists in the company");
		}
	}

	private void validateConversionFactor(ProductEntity product, BigDecimal conversionFactor) {
		if (conversionFactor.signum() <= 0) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "conversionFactor must be greater than zero");
		}

		if (!product.isStockable() && conversionFactor.compareTo(BigDecimal.ONE) != 0) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Non-stockable products must use conversionFactor 1");
		}
	}

	private String normalizeBarcode(String barcode) {
		if (barcode == null) {
			return null;
		}

		String trimmed = barcode.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String normalizeQuery(String query) {
		if (query == null) {
			return "";
		}

		String trimmed = query.trim();
		return trimmed.isEmpty() ? "" : trimmed;
	}
}
