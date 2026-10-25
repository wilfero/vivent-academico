package com.corpoviva.vivent.branches.service;

import com.corpoviva.vivent.branches.dto.BranchResponse;
import com.corpoviva.vivent.branches.dto.UpsertBranchRequest;
import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.branches.entity.LocationEntity;
import com.corpoviva.vivent.branches.entity.LocationType;
import com.corpoviva.vivent.branches.repository.BranchRepository;
import com.corpoviva.vivent.branches.repository.LocationRepository;
import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.subscriptions.service.SubscriptionPolicyService;
import jakarta.persistence.criteria.Predicate;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BranchService {

	private static final String COMPANY_ADMIN = "COMPANY_ADMIN";

	private final BranchRepository branchRepository;
	private final LocationRepository locationRepository;
	private final CompanyRepository companyRepository;
	private final SubscriptionPolicyService subscriptionPolicyService;

	public BranchService(
		BranchRepository branchRepository,
		LocationRepository locationRepository,
		CompanyRepository companyRepository,
		SubscriptionPolicyService subscriptionPolicyService
	) {
		this.branchRepository = branchRepository;
		this.locationRepository = locationRepository;
		this.companyRepository = companyRepository;
		this.subscriptionPolicyService = subscriptionPolicyService;
	}

	@Transactional(readOnly = true)
	public PageResponse<BranchResponse> listBranches(AuthenticatedUser authenticatedUser, int page, int pageSize, String query, Boolean active) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		Page<BranchEntity> branchPage = branchRepository.findAll(
			branchSearch(authenticatedUser, normalizeQuery(query), active),
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("name").ascending())
		);

		return PageResponse.from(branchPage.map(BranchResponse::from), normalizedPage, normalizedPageSize);
	}

	@Transactional(readOnly = true)
	public BranchResponse getBranch(AuthenticatedUser authenticatedUser, UUID branchId) {
		ensureCanAccessBranch(authenticatedUser, branchId);
		return BranchResponse.from(loadBranch(authenticatedUser.companyId(), branchId));
	}

	@Transactional
	public BranchResponse createBranch(AuthenticatedUser authenticatedUser, UpsertBranchRequest request) {
		CompanyEntity company = loadCompany(authenticatedUser.companyId());
		String normalizedCode = normalizeCode(request.code());

		if (branchRepository.existsByCompany_IdAndCodeIgnoreCaseAndDeletedAtIsNull(authenticatedUser.companyId(), normalizedCode)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Branch code already exists in the company");
		}

		if (request.active()) {
			subscriptionPolicyService.ensureBranchCapacity(authenticatedUser.companyId());
		}

		BranchEntity branch = new BranchEntity();
		branch.setCompany(company);
		branch.setName(request.name().trim());
		branch.setCode(normalizedCode);
		branch.setAddress(trimToNull(request.address()));
		branch.setPhone(trimToNull(request.phone()));
		branch.setEmail(trimToNull(request.email()));
		branch.setActive(request.active());

		BranchEntity savedBranch = branchRepository.save(branch);
		createDefaultLocation(company, savedBranch);

		return BranchResponse.from(savedBranch);
	}

	@Transactional
	public BranchResponse updateBranch(AuthenticatedUser authenticatedUser, UUID branchId, UpsertBranchRequest request) {
		BranchEntity branch = loadBranch(authenticatedUser.companyId(), branchId);
		String normalizedCode = normalizeCode(request.code());

		if (!branch.getCode().equalsIgnoreCase(normalizedCode)
			&& branchRepository.existsByCompany_IdAndCodeIgnoreCaseAndDeletedAtIsNull(authenticatedUser.companyId(), normalizedCode)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Branch code already exists in the company");
		}

		if (!branch.isActive() && request.active()) {
			subscriptionPolicyService.ensureBranchCapacity(authenticatedUser.companyId());
		}

		branch.setName(request.name().trim());
		branch.setCode(normalizedCode);
		branch.setAddress(trimToNull(request.address()));
		branch.setPhone(trimToNull(request.phone()));
		branch.setEmail(trimToNull(request.email()));
		branch.setActive(request.active());

		return BranchResponse.from(branch);
	}

	private void createDefaultLocation(CompanyEntity company, BranchEntity branch) {
		LocationEntity location = new LocationEntity();
		location.setCompany(company);
		location.setBranch(branch);
		location.setName("Main Warehouse");
		location.setCode("MAIN_WAREHOUSE");
		location.setType(LocationType.MAIN);
		location.setDefaultLocation(true);
		location.setActive(true);
		locationRepository.save(location);
	}

	private CompanyEntity loadCompany(UUID companyId) {
		return companyRepository.findByIdAndDeletedAtIsNull(companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Company not found"));
	}

	private BranchEntity loadBranch(UUID companyId, UUID branchId) {
		return branchRepository.findByIdAndCompany_IdAndDeletedAtIsNull(branchId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Branch not found"));
	}

	private Specification<BranchEntity> branchSearch(AuthenticatedUser authenticatedUser, String query, Boolean active) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			Predicate predicate = criteriaBuilder.and(
				criteriaBuilder.equal(root.get("company").get("id"), authenticatedUser.companyId()),
				criteriaBuilder.isNull(root.get("deletedAt"))
			);

			if (!canAccessAllBranches(authenticatedUser)) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("id"), authenticatedUser.branchId()));
			}

			if (active != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("active"), active));
			}

			if (!query.isBlank()) {
				String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
				Predicate queryPredicate = criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern)
				);
				predicate = criteriaBuilder.and(predicate, queryPredicate);
			}

			return predicate;
		};
	}

	private void ensureCanAccessBranch(AuthenticatedUser authenticatedUser, UUID branchId) {
		if (!canAccessAllBranches(authenticatedUser) && !authenticatedUser.branchId().equals(branchId)) {
			throw ApiException.forbidden(ErrorCode.PERMISSION_DENIED, "User cannot access another branch");
		}
	}

	private boolean canAccessAllBranches(AuthenticatedUser authenticatedUser) {
		return COMPANY_ADMIN.equals(authenticatedUser.roleCode());
	}

	private String normalizeCode(String code) {
		return code.trim().toUpperCase(Locale.ROOT);
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
