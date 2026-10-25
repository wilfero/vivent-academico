package com.corpoviva.vivent.purchases.service;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.purchases.dto.SupplierResponse;
import com.corpoviva.vivent.purchases.dto.UpsertSupplierRequest;
import com.corpoviva.vivent.purchases.entity.SupplierEntity;
import com.corpoviva.vivent.purchases.repository.SupplierRepository;
import com.corpoviva.vivent.security.AuthenticatedUser;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {

	private final SupplierRepository supplierRepository;
	private final CompanyRepository companyRepository;

	public SupplierService(SupplierRepository supplierRepository, CompanyRepository companyRepository) {
		this.supplierRepository = supplierRepository;
		this.companyRepository = companyRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<SupplierResponse> listSuppliers(AuthenticatedUser user, int page, int pageSize, String query, Boolean active) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		Page<SupplierEntity> supplierPage = supplierRepository.findAll(
			supplierSearch(user.companyId(), normalizeQuery(query), active),
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("name").ascending())
		);

		return PageResponse.from(supplierPage.map(SupplierResponse::from), normalizedPage, normalizedPageSize);
	}

	@Transactional(readOnly = true)
	public SupplierResponse getSupplier(AuthenticatedUser user, UUID supplierId) {
		return SupplierResponse.from(loadSupplier(user.companyId(), supplierId));
	}

	@Transactional
	public SupplierResponse createSupplier(AuthenticatedUser user, UpsertSupplierRequest request) {
		SupplierEntity supplier = new SupplierEntity();
		supplier.setCompany(loadCompany(user.companyId()));
		applyFields(supplier, request);
		return SupplierResponse.from(supplierRepository.save(supplier));
	}

	@Transactional
	public SupplierResponse updateSupplier(AuthenticatedUser user, UUID supplierId, UpsertSupplierRequest request) {
		SupplierEntity supplier = loadSupplier(user.companyId(), supplierId);
		applyFields(supplier, request);
		return SupplierResponse.from(supplier);
	}

	@Transactional
	public void deleteSupplier(AuthenticatedUser user, UUID supplierId) {
		SupplierEntity supplier = loadSupplier(user.companyId(), supplierId);
		supplier.setActive(false);
		supplier.setDeletedAt(Instant.now());
	}

	private SupplierEntity loadSupplier(UUID companyId, UUID supplierId) {
		return supplierRepository.findByIdAndCompany_IdAndDeletedAtIsNull(supplierId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Supplier not found"));
	}

	private CompanyEntity loadCompany(UUID companyId) {
		return companyRepository.findByIdAndDeletedAtIsNull(companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Company not found"));
	}

	private void applyFields(SupplierEntity supplier, UpsertSupplierRequest request) {
		supplier.setName(request.name().trim());
		supplier.setTaxId(trimToNull(request.taxId()));
		supplier.setEmail(trimToNull(request.email()));
		supplier.setPhone(trimToNull(request.phone()));
		supplier.setAddress(trimToNull(request.address()));
		supplier.setContactName(trimToNull(request.contactName()));
		supplier.setActive(request.active());
	}

	private String normalizeQuery(String query) {
		if (query == null) {
			return "";
		}
		String trimmed = query.trim().toLowerCase(Locale.ROOT);
		return trimmed.isEmpty() ? "" : trimmed;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private Specification<SupplierEntity> supplierSearch(UUID companyId, String query, Boolean active) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			Predicate predicate = criteriaBuilder.and(
				criteriaBuilder.equal(root.get("company").get("id"), companyId),
				criteriaBuilder.isNull(root.get("deletedAt"))
			);
			if (active != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("active"), active));
			}
			if (!query.isBlank()) {
				String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
				Predicate queryPredicate = criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
					criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("taxId"), "")), pattern),
					criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("contactName"), "")), pattern)
				);
				predicate = criteriaBuilder.and(predicate, queryPredicate);
			}
			return predicate;
		};
	}
}
