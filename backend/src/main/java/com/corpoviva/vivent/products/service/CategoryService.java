package com.corpoviva.vivent.products.service;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.products.dto.CategoryResponse;
import com.corpoviva.vivent.products.dto.UpsertCategoryRequest;
import com.corpoviva.vivent.products.entity.CategoryEntity;
import com.corpoviva.vivent.products.repository.CategoryRepository;
import com.corpoviva.vivent.security.AuthenticatedUser;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final CompanyRepository companyRepository;

	public CategoryService(CategoryRepository categoryRepository, CompanyRepository companyRepository) {
		this.categoryRepository = categoryRepository;
		this.companyRepository = companyRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<CategoryResponse> listCategories(AuthenticatedUser user, int page, int pageSize, String query, Boolean active) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		Page<CategoryEntity> categoryPage = categoryRepository.searchByCompany(
			user.companyId(),
			normalizeQuery(query),
			active,
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("name").ascending())
		);

		return PageResponse.from(categoryPage.map(CategoryResponse::from), normalizedPage, normalizedPageSize);
	}

	@Transactional(readOnly = true)
	public CategoryResponse getCategory(AuthenticatedUser user, UUID categoryId) {
		return CategoryResponse.from(loadCategory(user.companyId(), categoryId));
	}

	@Transactional
	public CategoryResponse createCategory(AuthenticatedUser user, UpsertCategoryRequest request) {
		CompanyEntity company = loadCompany(user.companyId());
		validateCategoryNameAvailable(company.getId(), request.name(), null);

		CategoryEntity category = new CategoryEntity();
		category.setCompany(company);
		category.setParentCategory(loadParentCategory(company.getId(), request.parentCategoryId(), null));
		category.setName(request.name().trim());
		category.setDescription(trimToNull(request.description()));
		category.setActive(request.active());

		return CategoryResponse.from(categoryRepository.save(category));
	}

	@Transactional
	public CategoryResponse updateCategory(AuthenticatedUser user, UUID categoryId, UpsertCategoryRequest request) {
		CategoryEntity category = loadCategory(user.companyId(), categoryId);

		if (!category.getName().equalsIgnoreCase(request.name().trim())) {
			validateCategoryNameAvailable(user.companyId(), request.name(), categoryId);
		}

		category.setParentCategory(loadParentCategory(user.companyId(), request.parentCategoryId(), categoryId));
		category.setName(request.name().trim());
		category.setDescription(trimToNull(request.description()));
		category.setActive(request.active());

		return CategoryResponse.from(category);
	}

	@Transactional
	public void deleteCategory(AuthenticatedUser user, UUID categoryId) {
		CategoryEntity category = loadCategory(user.companyId(), categoryId);
		category.setActive(false);
		category.setDeletedAt(Instant.now());
	}

	public CategoryEntity loadCategory(UUID companyId, UUID categoryId) {
		return categoryRepository.findByIdAndCompany_IdAndDeletedAtIsNull(categoryId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Category not found"));
	}

	private CategoryEntity loadParentCategory(UUID companyId, UUID parentCategoryId, UUID currentCategoryId) {
		if (parentCategoryId == null) {
			return null;
		}

		if (parentCategoryId.equals(currentCategoryId)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Category cannot be its own parent");
		}

		return loadCategory(companyId, parentCategoryId);
	}

	private CompanyEntity loadCompany(UUID companyId) {
		return companyRepository.findByIdAndDeletedAtIsNull(companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Company not found"));
	}

	private void validateCategoryNameAvailable(UUID companyId, String name, UUID ignoredCategoryId) {
		if (categoryRepository.existsByCompany_IdAndNameIgnoreCaseAndDeletedAtIsNull(companyId, name.trim())) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Category name already exists in the company");
		}
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
