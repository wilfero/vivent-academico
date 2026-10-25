package com.corpoviva.vivent.products.service;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.products.dto.ProductResponse;
import com.corpoviva.vivent.products.dto.UpsertProductRequest;
import com.corpoviva.vivent.products.entity.CategoryEntity;
import com.corpoviva.vivent.products.entity.ProductEntity;
import com.corpoviva.vivent.products.repository.ProductRepository;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.subscriptions.service.SubscriptionPolicyService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

	private final ProductRepository productRepository;
	private final CompanyRepository companyRepository;
	private final CategoryService categoryService;
	private final ProductUnitService productUnitService;
	private final SubscriptionPolicyService subscriptionPolicyService;

	public ProductService(
		ProductRepository productRepository,
		CompanyRepository companyRepository,
		CategoryService categoryService,
		ProductUnitService productUnitService,
		SubscriptionPolicyService subscriptionPolicyService
	) {
		this.productRepository = productRepository;
		this.companyRepository = companyRepository;
		this.categoryService = categoryService;
		this.productUnitService = productUnitService;
		this.subscriptionPolicyService = subscriptionPolicyService;
	}

	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> listProducts(AuthenticatedUser user, int page, int pageSize, String query, Boolean active, UUID categoryId) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		Page<ProductEntity> productPage = productRepository.searchByCompany(
			user.companyId(),
			normalizeQuery(query),
			active,
			categoryId,
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("name").ascending())
		);

		return PageResponse.from(productPage.map(ProductResponse::from), normalizedPage, normalizedPageSize);
	}

	@Transactional(readOnly = true)
	public ProductResponse getProduct(AuthenticatedUser user, UUID productId) {
		return ProductResponse.from(loadProduct(user.companyId(), productId));
	}

	@Transactional
	public ProductResponse createProduct(AuthenticatedUser user, UpsertProductRequest request) {
		CompanyEntity company = loadCompany(user.companyId());
		CategoryEntity category = loadCategory(user.companyId(), request.categoryId());
		productUnitService.loadActiveUnit(normalizeUnitCode(request.baseUnitCode()));
		validateSkuAvailable(user.companyId(), request.sku(), null);

		if (request.active()) {
			subscriptionPolicyService.ensureProductCapacity(user.companyId());
		}

		ProductEntity product = new ProductEntity();
		product.setCompany(company);
		applyProductFields(product, category, request);

		return ProductResponse.from(productRepository.save(product));
	}

	@Transactional
	public ProductResponse updateProduct(AuthenticatedUser user, UUID productId, UpsertProductRequest request) {
		ProductEntity product = loadProduct(user.companyId(), productId);
		CategoryEntity category = loadCategory(user.companyId(), request.categoryId());
		productUnitService.loadActiveUnit(normalizeUnitCode(request.baseUnitCode()));

		String newSku = normalizeSku(request.sku());
		if (newSku != null && !newSku.equalsIgnoreCase(product.getSku())) {
			validateSkuAvailable(user.companyId(), newSku, productId);
		}

		if (!product.isActive() && request.active()) {
			subscriptionPolicyService.ensureProductCapacity(user.companyId());
		}

		applyProductFields(product, category, request);
		return ProductResponse.from(product);
	}

	@Transactional
	public void deleteProduct(AuthenticatedUser user, UUID productId) {
		ProductEntity product = loadProduct(user.companyId(), productId);
		product.setActive(false);
		product.setDeletedAt(Instant.now());
	}

	public ProductEntity loadProduct(UUID companyId, UUID productId) {
		return productRepository.findByIdAndCompany_IdAndDeletedAtIsNull(productId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Product not found"));
	}

	private void applyProductFields(ProductEntity product, CategoryEntity category, UpsertProductRequest request) {
		product.setCategory(category);
		product.setName(request.name().trim());
		product.setDescription(trimToNull(request.description()));
		product.setBaseUnitCode(normalizeUnitCode(request.baseUnitCode()));
		product.setSku(normalizeSku(request.sku()));
		product.setMinimumStock(request.minimumStock() == null ? BigDecimal.ZERO : request.minimumStock());
		product.setStockable(request.stockable());
		product.setHasExpirationDate(request.hasExpirationDate());
		product.setHasLotTracking(request.hasLotTracking());
		product.setHasSerialTracking(request.hasSerialTracking());
		product.setActive(request.active());
	}

	private CompanyEntity loadCompany(UUID companyId) {
		return companyRepository.findByIdAndDeletedAtIsNull(companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Company not found"));
	}

	private CategoryEntity loadCategory(UUID companyId, UUID categoryId) {
		if (categoryId == null) {
			return null;
		}

		return categoryService.loadCategory(companyId, categoryId);
	}

	private void validateSkuAvailable(UUID companyId, String sku, UUID ignoredProductId) {
		String normalizedSku = normalizeSku(sku);
		if (normalizedSku != null && productRepository.existsByCompany_IdAndSkuIgnoreCaseAndDeletedAtIsNull(companyId, normalizedSku)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "SKU already exists in the company");
		}
	}

	private String normalizeUnitCode(String code) {
		return code.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizeSku(String sku) {
		String trimmed = trimToNull(sku);
		return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
	}

	private String normalizeQuery(String query) {
		if (query == null) {
			return "";
		}

		String trimmed = query.trim();
		return trimmed.isEmpty() ? "" : trimmed;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
