package com.corpoviva.vivent.platform.companies.service;

import com.corpoviva.vivent.branches.repository.BranchRepository;
import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.companies.dto.CompanyResponse;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.entity.CompanyStatus;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.platform.audit.service.PlatformAuditService;
import com.corpoviva.vivent.platform.companies.dto.PlatformCompanyResponse;
import com.corpoviva.vivent.platform.companies.dto.PlatformCompanySummaryResponse;
import com.corpoviva.vivent.platform.companies.dto.ResetAdminPasswordResponse;
import com.corpoviva.vivent.platform.companies.dto.UpdateCompanyStatusRequest;
import com.corpoviva.vivent.platform.companies.dto.UpdatePlatformCompanyRequest;
import com.corpoviva.vivent.platform.companies.dto.UpdateSubscriptionRequest;
import com.corpoviva.vivent.platform.plans.repository.PlatformPlanRepository;
import com.corpoviva.vivent.platform.util.SecurePasswordGenerator;
import com.corpoviva.vivent.products.repository.ProductRepository;
import com.corpoviva.vivent.subscriptions.dto.SubscriptionResponse;
import com.corpoviva.vivent.subscriptions.entity.PlanEntity;
import com.corpoviva.vivent.subscriptions.entity.SubscriptionEntity;
import com.corpoviva.vivent.subscriptions.repository.SubscriptionRepository;
import com.corpoviva.vivent.users.entity.UserEntity;
import com.corpoviva.vivent.users.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformCompanyService {

	private final CompanyRepository companyRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final BranchRepository branchRepository;
	private final UserRepository userRepository;
	private final ProductRepository productRepository;
	private final PlatformPlanRepository platformPlanRepository;
	private final PlatformAuditService platformAuditService;
	private final PasswordEncoder passwordEncoder;
	private final com.corpoviva.vivent.auth.service.RefreshTokenService refreshTokenService;

	public PlatformCompanyService(
		CompanyRepository companyRepository,
		SubscriptionRepository subscriptionRepository,
		BranchRepository branchRepository,
		UserRepository userRepository,
		ProductRepository productRepository,
		PlatformPlanRepository platformPlanRepository,
		PlatformAuditService platformAuditService,
		PasswordEncoder passwordEncoder,
		com.corpoviva.vivent.auth.service.RefreshTokenService refreshTokenService
	) {
		this.companyRepository = companyRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.branchRepository = branchRepository;
		this.userRepository = userRepository;
		this.productRepository = productRepository;
		this.platformPlanRepository = platformPlanRepository;
		this.platformAuditService = platformAuditService;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenService = refreshTokenService;
	}

	@Transactional(readOnly = true)
	public PageResponse<PlatformCompanySummaryResponse> listCompanies(
		int page,
		int pageSize,
		String query,
		CompanyStatus status
	) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);

		Page<CompanyEntity> companyPage = companyRepository.findAll(
			companySearch(normalizeQuery(query), status),
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("createdAt").descending())
		);

		Map<UUID, SubscriptionEntity> subscriptions = latestSubscriptionsByCompany(companyPage.getContent());

		Page<PlatformCompanySummaryResponse> responsePage = companyPage.map(company -> {
			SubscriptionEntity subscription = subscriptions.get(company.getId());
			String planCode = subscription == null || subscription.getPlan() == null ? null : subscription.getPlan().getCode();
			String planName = subscription == null || subscription.getPlan() == null ? null : subscription.getPlan().getName();
			String subscriptionStatus = subscription == null ? null : subscription.getStatus().name();

			return PlatformCompanySummaryResponse.from(company, planCode, planName, subscriptionStatus);
		});

		return PageResponse.from(responsePage, normalizedPage, normalizedPageSize);
	}

	@Transactional(readOnly = true)
	public PlatformCompanyResponse getCompany(UUID companyId) {
		CompanyEntity company = loadCompany(companyId);
		SubscriptionEntity subscription = subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(companyId).orElse(null);

		long branches = branchRepository.countActiveByCompanyId(companyId);
		long users = userRepository.countActiveByCompanyId(companyId);
		long products = productRepository.countActiveByCompanyId(companyId);

		List<UserEntity> admins = userRepository.findCompanyAdminsByCompanyId(companyId);
		PlatformCompanyResponse.AdminUserResponse admin = admins.stream()
			.findFirst()
			.map(user -> new PlatformCompanyResponse.AdminUserResponse(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getFirstName(),
				user.getLastName()
			))
			.orElse(null);

		return new PlatformCompanyResponse(
			CompanyResponse.from(company),
			subscription == null ? null : SubscriptionResponse.from(subscription),
			new PlatformCompanyResponse.UsageResponse(branches, users, products),
			admin
		);
	}

	@Transactional
	public PlatformCompanyResponse updateCompany(
		UUID companyId,
		UpdatePlatformCompanyRequest request,
		UUID operatorId,
		String ipAddress,
		String userAgent
	) {
		CompanyEntity company = loadCompany(companyId);
		LinkedHashSet<String> locales = validateLocales(request);

		company.setLegalName(request.legalName().trim());
		company.setCommercialName(request.commercialName().trim());
		company.setLogoUrl(trimToNull(request.logoUrl()));
		company.setPrimaryColor(request.primaryColor());
		company.setSecondaryColor(request.secondaryColor());
		company.setDefaultTheme(request.defaultTheme());
		company.setSupportedLocales(locales.toArray(String[]::new));
		company.setDefaultLocale(request.defaultLocale().trim());
		company.setCurrencyCode(request.currencyCode().trim().toUpperCase(Locale.ROOT));
		company.setTimezone(request.timezone().trim());
		company.setTaxEnabled(request.taxEnabled());
		company.setTaxRate(request.taxRate());

		platformAuditService.log(operatorId, "UPDATE", "company", companyId, null, null, ipAddress, userAgent);

		return getCompany(companyId);
	}

	@Transactional
	public PlatformCompanySummaryResponse updateStatus(
		UUID companyId,
		UpdateCompanyStatusRequest request,
		UUID operatorId,
		String ipAddress,
		String userAgent
	) {
		if (request.status() == CompanyStatus.DELETED) {
			throw ApiException.badRequest(ErrorCode.INVALID_PLATFORM_STATUS, "Companies cannot be deleted through the platform");
		}

		CompanyEntity company = loadCompany(companyId);
		CompanyStatus previous = company.getStatus();
		company.setStatus(request.status());

		platformAuditService.log(
			operatorId,
			"STATUS_CHANGE",
			"company",
			companyId,
			previous.name(),
			request.status().name(),
			ipAddress,
			userAgent
		);

		SubscriptionEntity subscription = subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(companyId).orElse(null);
		return PlatformCompanySummaryResponse.from(
			company,
			subscription == null || subscription.getPlan() == null ? null : subscription.getPlan().getCode(),
			subscription == null || subscription.getPlan() == null ? null : subscription.getPlan().getName(),
			subscription == null ? null : subscription.getStatus().name()
		);
	}

	@Transactional
	public SubscriptionResponse updateSubscription(
		UUID companyId,
		UpdateSubscriptionRequest request,
		UUID operatorId,
		String ipAddress,
		String userAgent
	) {
		loadCompany(companyId);

		PlanEntity plan = platformPlanRepository.findById(request.planId())
			.filter(PlanEntity::isActive)
			.orElseThrow(() -> ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Selected plan is not available"));

		SubscriptionEntity subscription = subscriptionRepository.findFirstByCompanyIdOrderByCreatedAtDesc(companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Company subscription not found"));

		subscription.setPlanId(plan.getId());
		subscription.setPlan(plan);
		subscription.setStatus(request.status());
		subscription.setEndsAt(request.endsAt());
		subscription.setTrialEndsAt(request.trialEndsAt());
		subscription.setNotes(trimToNull(request.notes()));

		platformAuditService.log(
			operatorId,
			"SUBSCRIPTION_CHANGE",
			"company",
			companyId,
			null,
			"{\"planCode\":\"" + plan.getCode() + "\",\"status\":\"" + request.status().name() + "\"}",
			ipAddress,
			userAgent
		);

		return SubscriptionResponse.from(subscription);
	}

	@Transactional
	public ResetAdminPasswordResponse resetAdminPassword(
		UUID companyId,
		UUID operatorId,
		String ipAddress,
		String userAgent
	) {
		loadCompany(companyId);

		UserEntity admin = userRepository.findCompanyAdminsByCompanyId(companyId)
			.stream()
			.findFirst()
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Company has no administrator user"));

		String newPassword = SecurePasswordGenerator.generate();
		admin.setPasswordHash(passwordEncoder.encode(newPassword));
		admin.setMustChangePassword(true);
		admin.setAuthVersion(admin.getAuthVersion() + 1);
		refreshTokenService.revokeAllForUser(admin.getId());

		platformAuditService.log(
			operatorId,
			"RESET_ADMIN_PASSWORD",
			"company",
			companyId,
			null,
			null,
			ipAddress,
			userAgent
		);

		return new ResetAdminPasswordResponse(admin.getId(), admin.getUsername(), newPassword);
	}

	private CompanyEntity loadCompany(UUID companyId) {
		return companyRepository.findByIdAndDeletedAtIsNull(companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Company not found"));
	}

	private Map<UUID, SubscriptionEntity> latestSubscriptionsByCompany(List<CompanyEntity> companies) {
		List<UUID> companyIds = companies.stream().map(CompanyEntity::getId).toList();

		if (companyIds.isEmpty()) {
			return Map.of();
		}

		Map<UUID, SubscriptionEntity> latest = new LinkedHashMap<>();
		subscriptionRepository.findLatestByCompanyIdIn(companyIds).forEach(subscription ->
			latest.putIfAbsent(subscription.getCompanyId(), subscription)
		);
		return latest;
	}

	private LinkedHashSet<String> validateLocales(UpdatePlatformCompanyRequest request) {
		LinkedHashSet<String> locales = new LinkedHashSet<>();
		request.supportedLocales().stream().map(String::trim).forEach(locales::add);

		if (!locales.contains(request.defaultLocale().trim())) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "defaultLocale must be included in supportedLocales");
		}

		if (locales.isEmpty()) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "supportedLocales cannot be empty");
		}

		return locales;
	}

	private Specification<CompanyEntity> companySearch(String query, CompanyStatus status) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			Predicate predicate = criteriaBuilder.isNull(root.get("deletedAt"));

			if (status != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
			}

			if (query != null) {
				String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
				Predicate queryPredicate = criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("legalName")), pattern),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("commercialName")), pattern),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("tenantSlug")), pattern)
				);
				predicate = criteriaBuilder.and(predicate, queryPredicate);
			}

			return predicate;
		};
	}

	private String normalizeQuery(String query) {
		if (query == null) {
			return null;
		}

		String trimmed = query.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
