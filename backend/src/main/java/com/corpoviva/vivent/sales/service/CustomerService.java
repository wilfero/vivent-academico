package com.corpoviva.vivent.sales.service;

import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.sales.dto.CustomerResponse;
import com.corpoviva.vivent.sales.dto.UpsertCustomerRequest;
import com.corpoviva.vivent.sales.entity.CustomerEntity;
import com.corpoviva.vivent.sales.repository.CustomerRepository;
import com.corpoviva.vivent.security.AuthenticatedUser;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final CompanyRepository companyRepository;

	public CustomerService(CustomerRepository customerRepository, CompanyRepository companyRepository) {
		this.customerRepository = customerRepository;
		this.companyRepository = companyRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<CustomerResponse> listCustomers(AuthenticatedUser user, int page, int pageSize, String query, Boolean active) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		Page<CustomerEntity> customerPage = customerRepository.findAll(
			customerSearch(user.companyId(), normalizeQuery(query), active),
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("name").ascending())
		);

		return PageResponse.from(customerPage.map(CustomerResponse::from), normalizedPage, normalizedPageSize);
	}

	@Transactional(readOnly = true)
	public CustomerResponse getCustomer(AuthenticatedUser user, UUID customerId) {
		return CustomerResponse.from(loadCustomer(user.companyId(), customerId));
	}

	@Transactional
	public CustomerResponse createCustomer(AuthenticatedUser user, UpsertCustomerRequest request) {
		CustomerEntity customer = new CustomerEntity();
		customer.setCompany(loadCompany(user.companyId()));
		applyFields(customer, request);
		return CustomerResponse.from(customerRepository.save(customer));
	}

	@Transactional
	public CustomerResponse updateCustomer(AuthenticatedUser user, UUID customerId, UpsertCustomerRequest request) {
		CustomerEntity customer = loadCustomer(user.companyId(), customerId);
		applyFields(customer, request);
		return CustomerResponse.from(customer);
	}

	@Transactional
	public void deleteCustomer(AuthenticatedUser user, UUID customerId) {
		CustomerEntity customer = loadCustomer(user.companyId(), customerId);
		customer.setActive(false);
		customer.setDeletedAt(Instant.now());
	}

	private CustomerEntity loadCustomer(UUID companyId, UUID customerId) {
		return customerRepository.findByIdAndCompany_IdAndDeletedAtIsNull(customerId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Customer not found"));
	}

	private CompanyEntity loadCompany(UUID companyId) {
		return companyRepository.findByIdAndDeletedAtIsNull(companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Company not found"));
	}

	private void applyFields(CustomerEntity customer, UpsertCustomerRequest request) {
		customer.setName(request.name().trim());
		customer.setTaxId(trimToNull(request.taxId()));
		customer.setEmail(trimToNull(request.email()));
		customer.setPhone(trimToNull(request.phone()));
		customer.setAddress(trimToNull(request.address()));
		customer.setGenericConsumer(request.genericConsumer());
		customer.setActive(request.active());
	}

	private Specification<CustomerEntity> customerSearch(UUID companyId, String query, Boolean active) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			Predicate predicate = criteriaBuilder.and(
				criteriaBuilder.equal(root.get("company").get("id"), companyId),
				criteriaBuilder.isNull(root.get("deletedAt"))
			);
			if (active != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("active"), active));
			}
			if (!query.isBlank()) {
				String pattern = "%" + query + "%";
				Predicate queryPredicate = criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
					criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("taxId"), "")), pattern),
					criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("email"), "")), pattern)
				);
				predicate = criteriaBuilder.and(predicate, queryPredicate);
			}
			return predicate;
		};
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
}
