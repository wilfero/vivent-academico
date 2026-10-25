package com.corpoviva.vivent.platform.companies.service;

import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.branches.entity.LocationEntity;
import com.corpoviva.vivent.branches.entity.LocationType;
import com.corpoviva.vivent.branches.repository.BranchRepository;
import com.corpoviva.vivent.branches.repository.LocationRepository;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.entity.CompanyStatus;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.permissions.entity.RoleEntity;
import com.corpoviva.vivent.permissions.repository.RoleRepository;
import com.corpoviva.vivent.platform.audit.service.PlatformAuditService;
import com.corpoviva.vivent.platform.companies.dto.CreatePlatformCompanyRequest;
import com.corpoviva.vivent.platform.companies.dto.ProvisionedCompanyResponse;
import com.corpoviva.vivent.platform.plans.repository.PlatformPlanRepository;
import com.corpoviva.vivent.platform.util.SecurePasswordGenerator;
import com.corpoviva.vivent.subscriptions.entity.PlanEntity;
import com.corpoviva.vivent.subscriptions.entity.SubscriptionEntity;
import com.corpoviva.vivent.subscriptions.entity.SubscriptionStatus;
import com.corpoviva.vivent.subscriptions.repository.SubscriptionRepository;
import com.corpoviva.vivent.tenant.config.TenantProperties;
import com.corpoviva.vivent.users.entity.UserBranchAssignmentEntity;
import com.corpoviva.vivent.users.entity.UserBranchAssignmentStatus;
import com.corpoviva.vivent.users.entity.UserEntity;
import com.corpoviva.vivent.users.entity.UserStatus;
import com.corpoviva.vivent.users.repository.UserBranchAssignmentRepository;
import com.corpoviva.vivent.users.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformCompanyProvisioningService {

	private static final Set<String> RESERVED_TENANT_SLUGS = Set.of(
		"admin", "api", "app", "billing", "blog", "help", "master", "platform",
		"root", "status", "support", "system", "vivent", "corpoviva", "www"
	);
	private static final String COMPANY_ADMIN_ROLE_CODE = "COMPANY_ADMIN";
	private static final String DEFAULT_MAIN_LOCATION_CODE = "MAIN_WAREHOUSE";
	private static final long TRIAL_DAYS = 30;

	private final CompanyRepository companyRepository;
	private final BranchRepository branchRepository;
	private final LocationRepository locationRepository;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserBranchAssignmentRepository userBranchAssignmentRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final PlatformPlanRepository platformPlanRepository;
	private final PlatformAuditService platformAuditService;
	private final TenantProperties tenantProperties;
	private final PasswordEncoder passwordEncoder;

	public PlatformCompanyProvisioningService(
		CompanyRepository companyRepository,
		BranchRepository branchRepository,
		LocationRepository locationRepository,
		UserRepository userRepository,
		RoleRepository roleRepository,
		UserBranchAssignmentRepository userBranchAssignmentRepository,
		SubscriptionRepository subscriptionRepository,
		PlatformPlanRepository platformPlanRepository,
		PlatformAuditService platformAuditService,
		TenantProperties tenantProperties,
		PasswordEncoder passwordEncoder
	) {
		this.companyRepository = companyRepository;
		this.branchRepository = branchRepository;
		this.locationRepository = locationRepository;
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userBranchAssignmentRepository = userBranchAssignmentRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.platformPlanRepository = platformPlanRepository;
		this.platformAuditService = platformAuditService;
		this.tenantProperties = tenantProperties;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public ProvisionedCompanyResponse provision(
		CreatePlatformCompanyRequest request,
		UUID operatorId,
		String ipAddress,
		String userAgent
	) {
		String tenantSlug = normalizeTenantSlug(request.tenantSlug());
		ensureTenantSlugAvailable(tenantSlug);

		PlanEntity plan = platformPlanRepository.findById(request.planId())
			.filter(PlanEntity::isActive)
			.orElseThrow(() -> ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Selected plan is not available"));

		RoleEntity adminRole = roleRepository.findByCodeAndCompanyIdIsNull(COMPANY_ADMIN_ROLE_CODE)
			.orElseThrow(() -> ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Company admin role is not available"));

		CompanyEntity company = new CompanyEntity();
		company.setLegalName(request.legalName().trim());
		company.setCommercialName(request.commercialName().trim());
		company.setTenantSlug(tenantSlug);
		company.setDomain(tenantSlug + "." + tenantProperties.publicBaseDomain());
		company.setLogoUrl(trimToNull(request.logoUrl()));
		company.setPrimaryColor(defaultIfBlank(request.primaryColor(), "#2563eb"));
		company.setSecondaryColor(defaultIfBlank(request.secondaryColor(), "#64748b"));
		company.setDefaultTheme(defaultIfBlank(request.defaultTheme(), "light"));
		company.setSupportedLocales(normalizeLocales(request));
		company.setDefaultLocale(defaultIfBlank(request.defaultLocale(), "es-GT"));
		company.setCurrencyCode(defaultIfBlank(request.currencyCode(), "GTQ").toUpperCase(Locale.ROOT));
		company.setTimezone(defaultIfBlank(request.timezone(), "America/Guatemala"));
		company.setTaxEnabled(Boolean.TRUE.equals(request.taxEnabled()));
		company.setTaxRate(request.taxRate() == null ? BigDecimal.ZERO : request.taxRate());
		company.setStatus(CompanyStatus.ACTIVE);

		CompanyEntity savedCompany = companyRepository.save(company);

		BranchEntity branch = new BranchEntity();
		branch.setCompany(savedCompany);
		branch.setName(request.branchName().trim());
		branch.setCode(request.branchCode().trim().toUpperCase(Locale.ROOT));
		branch.setActive(true);
		BranchEntity savedBranch = branchRepository.save(branch);

		LocationEntity location = new LocationEntity();
		location.setCompany(savedCompany);
		location.setBranch(savedBranch);
		location.setName("Main Warehouse");
		location.setCode(DEFAULT_MAIN_LOCATION_CODE);
		location.setType(LocationType.MAIN);
		location.setDefaultLocation(true);
		location.setActive(true);
		locationRepository.save(location);

		String initialPassword = SecurePasswordGenerator.generate();

		UserEntity admin = new UserEntity();
		admin.setCompany(savedCompany);
		admin.setBranch(savedBranch);
		admin.setRole(adminRole);
		admin.setUsername(request.adminUsername().trim());
		admin.setPasswordHash(passwordEncoder.encode(initialPassword));
		admin.setMustChangePassword(true);
		admin.setFirstName(request.adminFirstName().trim());
		admin.setLastName(request.adminLastName().trim());
		admin.setEmail(request.adminEmail().trim().toLowerCase(Locale.ROOT));
		admin.setStatus(UserStatus.ACTIVE);
		UserEntity savedAdmin = userRepository.save(admin);

		UserBranchAssignmentEntity assignment = new UserBranchAssignmentEntity();
		assignment.setUser(savedAdmin);
		assignment.setBranch(savedBranch);
		assignment.setPrimary(true);
		assignment.setStatus(UserBranchAssignmentStatus.ACTIVE);
		assignment.setStartAt(Instant.now());
		userBranchAssignmentRepository.save(assignment);

		SubscriptionEntity subscription = new SubscriptionEntity();
		subscription.setCompanyId(savedCompany.getId());
		subscription.setPlanId(plan.getId());
		subscription.setStatus(SubscriptionStatus.TRIAL);
		subscription.setStartsAt(Instant.now());
		subscription.setTrialEndsAt(Instant.now().plus(TRIAL_DAYS, ChronoUnit.DAYS));
		subscriptionRepository.save(subscription);

		platformAuditService.log(
			operatorId,
			"PROVISION",
			"company",
			savedCompany.getId(),
			null,
			"{\"tenantSlug\":\"" + tenantSlug + "\",\"planCode\":\"" + plan.getCode() + "\"}",
			ipAddress,
			userAgent
		);

		return new ProvisionedCompanyResponse(
			savedCompany.getId(),
			savedCompany.getTenantSlug(),
			savedCompany.getDomain(),
			savedAdmin.getUsername(),
			initialPassword
		);
	}

	private String normalizeTenantSlug(String tenantSlug) {
		return tenantSlug.trim().toLowerCase(Locale.ROOT);
	}

	private void ensureTenantSlugAvailable(String tenantSlug) {
		if (RESERVED_TENANT_SLUGS.contains(tenantSlug)) {
			throw ApiException.badRequest(ErrorCode.TENANT_SLUG_UNAVAILABLE, "The tenant slug is reserved");
		}

		if (companyRepository.existsByTenantSlug(tenantSlug)) {
			throw ApiException.badRequest(ErrorCode.TENANT_SLUG_UNAVAILABLE, "The tenant slug is already in use");
		}

		String domain = tenantSlug + "." + tenantProperties.publicBaseDomain();
		if (companyRepository.existsByDomainIgnoreCase(domain)) {
			throw ApiException.badRequest(ErrorCode.TENANT_SLUG_UNAVAILABLE, "The tenant domain is already in use");
		}
	}

	private String[] normalizeLocales(CreatePlatformCompanyRequest request) {
		LinkedHashSet<String> locales = new LinkedHashSet<>();
		String defaultLocale = defaultIfBlank(request.defaultLocale(), "es-GT");
		locales.add(defaultLocale);

		if (request.supportedLocales() != null) {
			request.supportedLocales().stream().map(String::trim).filter(value -> !value.isEmpty()).forEach(locales::add);
		}

		if (locales.isEmpty()) {
			locales.add(defaultLocale);
		}

		return locales.toArray(String[]::new);
	}

	private String defaultIfBlank(String value, String fallback) {
		if (value == null) {
			return fallback;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? fallback : trimmed;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
