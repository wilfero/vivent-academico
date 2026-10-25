package com.corpoviva.vivent.purchases.service;

import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.branches.repository.BranchRepository;
import com.corpoviva.vivent.audit.entity.AuditAction;
import com.corpoviva.vivent.audit.service.AuditLogService;
import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.inventory.entity.StockMovementType;
import com.corpoviva.vivent.inventory.service.InventoryService;
import com.corpoviva.vivent.products.entity.ProductEntity;
import com.corpoviva.vivent.products.entity.ProductPresentationEntity;
import com.corpoviva.vivent.products.repository.ProductPresentationRepository;
import com.corpoviva.vivent.purchases.dto.PurchaseDetailRequest;
import com.corpoviva.vivent.purchases.dto.PurchaseDetailResponse;
import com.corpoviva.vivent.purchases.dto.PurchaseResponse;
import com.corpoviva.vivent.purchases.dto.UpsertPurchaseRequest;
import com.corpoviva.vivent.purchases.dto.VoidPurchaseRequest;
import com.corpoviva.vivent.purchases.entity.PurchaseDetailEntity;
import com.corpoviva.vivent.purchases.entity.PurchaseEntity;
import com.corpoviva.vivent.purchases.entity.PurchaseStatus;
import com.corpoviva.vivent.purchases.entity.SupplierEntity;
import com.corpoviva.vivent.purchases.repository.PurchaseDetailRepository;
import com.corpoviva.vivent.purchases.repository.PurchaseRepository;
import com.corpoviva.vivent.purchases.repository.SupplierRepository;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.users.entity.UserEntity;
import com.corpoviva.vivent.users.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {

	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final String COMPANY_ADMIN = "COMPANY_ADMIN";
	private static final String PURCHASE_DETAIL_SOURCE = "PURCHASE_DETAIL";
	private static final String PURCHASE_VOID_SOURCE = "PURCHASE_VOID";

	private final PurchaseRepository purchaseRepository;
	private final PurchaseDetailRepository purchaseDetailRepository;
	private final SupplierRepository supplierRepository;
	private final CompanyRepository companyRepository;
	private final BranchRepository branchRepository;
	private final ProductPresentationRepository productPresentationRepository;
	private final InventoryService inventoryService;
	private final AuditLogService auditLogService;
	private final UserRepository userRepository;

	public PurchaseService(
		PurchaseRepository purchaseRepository,
		PurchaseDetailRepository purchaseDetailRepository,
		SupplierRepository supplierRepository,
		CompanyRepository companyRepository,
		BranchRepository branchRepository,
		ProductPresentationRepository productPresentationRepository,
		InventoryService inventoryService,
		AuditLogService auditLogService,
		UserRepository userRepository
	) {
		this.purchaseRepository = purchaseRepository;
		this.purchaseDetailRepository = purchaseDetailRepository;
		this.supplierRepository = supplierRepository;
		this.companyRepository = companyRepository;
		this.branchRepository = branchRepository;
		this.productPresentationRepository = productPresentationRepository;
		this.inventoryService = inventoryService;
		this.auditLogService = auditLogService;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<PurchaseResponse> listPurchases(
		AuthenticatedUser user,
		int page,
		int pageSize,
		UUID branchId,
		UUID supplierId,
		PurchaseStatus status,
		LocalDate fromDate,
		LocalDate toDate,
		String query
	) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		UUID scopedBranchId = scopedBranchId(user, branchId);
		Page<PurchaseEntity> purchasePage = purchaseRepository.findAll(
			purchaseSearch(user.companyId(), scopedBranchId, supplierId, status, fromDate, toDate, normalizeQuery(query)),
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("createdAt").descending())
		);

		Map<UUID, String> usernames = usernameMap(user.companyId(), purchasePage.getContent());
		return PageResponse.from(purchasePage.map(purchase -> PurchaseResponse.from(purchase, List.of(), usernames)), normalizedPage, normalizedPageSize);
	}

	@Transactional(readOnly = true)
	public PurchaseResponse getPurchase(AuthenticatedUser user, UUID purchaseId) {
		PurchaseEntity purchase = loadPurchase(user, purchaseId);
		return toResponse(purchase);
	}

	@Transactional
	public PurchaseResponse createPurchase(AuthenticatedUser user, UpsertPurchaseRequest request) {
		CompanyEntity company = loadCompany(user.companyId());
		BranchEntity branch = loadBranch(user, request.branchId() == null ? user.branchId() : request.branchId());
		SupplierEntity supplier = loadSupplier(user.companyId(), request.supplierId());
		PurchaseStatus status = normalizeWritableStatus(request.status());
		String purchaseNumber = normalizePurchaseNumber(request.purchaseNumber());
		if (purchaseNumber == null) {
			purchaseNumber = generatePurchaseNumber(user.companyId());
		} else {
			validatePurchaseNumberAvailable(user.companyId(), purchaseNumber);
		}

		PurchaseEntity purchase = new PurchaseEntity();
		purchase.setCompany(company);
		purchase.setBranch(branch);
		purchase.setSupplier(supplier);
		purchase.setPurchaseNumber(purchaseNumber);
		purchase.setCreatedBy(user.userId());
		purchase.setTimezoneSnapshot(resolveCompanyTimezone(company));
		purchase.setSubtotal(ZERO);
		purchase.setTaxAmount(ZERO);
		purchase.setTotalAmount(ZERO);
		applyPurchaseFields(purchase, request, status);
		PurchaseEntity savedPurchase = purchaseRepository.save(purchase);

		saveDetails(user.companyId(), savedPurchase, request.details());
		recalculateTotals(savedPurchase);
		return toResponse(savedPurchase);
	}

	@Transactional
	public PurchaseResponse updatePurchase(AuthenticatedUser user, UUID purchaseId, UpsertPurchaseRequest request) {
		PurchaseEntity purchase = loadPurchase(user, purchaseId);
		ensureWritable(purchase);
		BranchEntity branch = loadBranch(user, request.branchId() == null ? user.branchId() : request.branchId());
		SupplierEntity supplier = loadSupplier(user.companyId(), request.supplierId());
		PurchaseStatus status = normalizeWritableStatus(request.status());
		String purchaseNumber = normalizePurchaseNumber(request.purchaseNumber());
		if (purchaseNumber != null && !purchaseNumber.equalsIgnoreCase(purchase.getPurchaseNumber())) {
			validatePurchaseNumberAvailable(user.companyId(), purchaseNumber);
			purchase.setPurchaseNumber(purchaseNumber);
		}

		purchase.setBranch(branch);
		purchase.setSupplier(supplier);
		applyPurchaseFields(purchase, request, status);
		purchaseDetailRepository.deleteByPurchase_Id(purchase.getId());
		saveDetails(user.companyId(), purchase, request.details());
		recalculateTotals(purchase);
		return toResponse(purchase);
	}

	@Transactional
	public PurchaseResponse confirmPurchase(AuthenticatedUser user, UUID purchaseId) {
		PurchaseEntity purchase = loadPurchaseForUpdate(user, purchaseId);
		PurchaseStatus oldStatus = purchase.getStatus();
		if (purchase.getStatus() == PurchaseStatus.CONFIRMED) {
			throw ApiException.badRequest(ErrorCode.INVALID_PURCHASE_STATUS, "Purchase is already confirmed");
		}
		if (purchase.getStatus() == PurchaseStatus.VOIDED) {
			throw ApiException.badRequest(ErrorCode.INVALID_PURCHASE_STATUS, "Voided purchase cannot be confirmed");
		}

		List<PurchaseDetailEntity> details = purchaseDetailRepository.findByPurchase_IdOrderByCreatedAtAsc(purchase.getId());
		if (details.isEmpty()) {
			throw ApiException.badRequest(ErrorCode.INVALID_PURCHASE_STATUS, "Purchase must have at least one detail");
		}

		for (PurchaseDetailEntity detail : details) {
			inventoryService.receiveStock(
				user,
				purchase.getBranch().getId(),
				null,
				detail.getProduct().getId(),
				detail.getQuantityBase(),
				detail.getUnitCostBase(),
				detail.getExpirationDate(),
				detail.getLotNumber(),
				StockMovementType.PURCHASE_IN,
				PURCHASE_DETAIL_SOURCE,
				detail.getId(),
				detail.getId(),
				"Purchase " + purchase.getPurchaseNumber()
			);
		}

		purchase.setStatus(PurchaseStatus.CONFIRMED);
		purchase.setConfirmedBy(user.userId());
		purchase.setConfirmedAt(Instant.now());
		auditLogService.log(user, purchase.getBranch().getId(), AuditAction.CONFIRM, "purchase", purchase.getId(), jsonStatus(oldStatus), jsonStatus(purchase.getStatus()));
		return toResponse(purchase);
	}

	@Transactional
	public PurchaseResponse voidPurchase(AuthenticatedUser user, UUID purchaseId, VoidPurchaseRequest request) {
		PurchaseEntity purchase = loadPurchaseForUpdate(user, purchaseId);
		PurchaseStatus oldStatus = purchase.getStatus();
		if (purchase.getStatus() == PurchaseStatus.VOIDED) {
			throw ApiException.badRequest(ErrorCode.INVALID_PURCHASE_STATUS, "Purchase is already voided");
		}

		String reason = trimToNull(request.reason());
		if (reason == null) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Void reason is required");
		}

		if (purchase.getStatus() == PurchaseStatus.CONFIRMED) {
			List<PurchaseDetailEntity> details = purchaseDetailRepository.findByPurchase_IdOrderByCreatedAtAsc(purchase.getId());
			for (PurchaseDetailEntity detail : details) {
				inventoryService.voidPurchaseReceipt(
					user,
					purchase.getBranch().getId(),
					null,
					detail.getProduct().getId(),
					detail.getId(),
					detail.getQuantityBase(),
					PURCHASE_VOID_SOURCE,
					detail.getId(),
					reason
				);
			}
		}

		purchase.setStatus(PurchaseStatus.VOIDED);
		purchase.setVoidedBy(user.userId());
		purchase.setVoidReason(reason);
		purchase.setVoidedAt(Instant.now());
		auditLogService.log(user, purchase.getBranch().getId(), AuditAction.VOID, "purchase", purchase.getId(), jsonStatus(oldStatus), jsonStatus(purchase.getStatus()));
		return toResponse(purchase);
	}

	private void applyPurchaseFields(PurchaseEntity purchase, UpsertPurchaseRequest request, PurchaseStatus status) {
		purchase.setSupplierInvoiceNumber(trimToNull(request.supplierInvoiceNumber()));
		purchase.setPurchaseDate(request.purchaseDate());
		purchase.setPaymentMethod(request.paymentMethod());
		purchase.setPaymentType(request.paymentType());
		purchase.setDiscountAmount(nonNull(request.discountAmount()));
		purchase.setStatus(status);
		purchase.setNotes(trimToNull(request.notes()));
	}

	private void saveDetails(UUID companyId, PurchaseEntity purchase, List<PurchaseDetailRequest> detailRequests) {
		for (PurchaseDetailRequest detailRequest : detailRequests) {
			ProductPresentationEntity presentation = productPresentationRepository
				.findByIdAndCompany_IdAndDeletedAtIsNull(detailRequest.presentationId(), companyId)
				.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Presentation not found"));
			ProductEntity product = presentation.getProduct();
			if (!presentation.isActive() || !product.isActive() || !product.isStockable()) {
				throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Presentation product must be active and stockable");
			}

			BigDecimal conversionFactor = presentation.getConversionFactor();
			BigDecimal quantityPresentation = detailRequest.quantityPresentation();
			BigDecimal quantityBase = quantityPresentation.multiply(conversionFactor);
			BigDecimal unitCostPresentation = detailRequest.unitCostPresentation();
			BigDecimal unitCostBase = unitCostPresentation.divide(conversionFactor, 4, RoundingMode.HALF_UP);
			BigDecimal subtotal = quantityPresentation.multiply(unitCostPresentation);
			BigDecimal taxAmount = nonNull(detailRequest.taxAmount());

			PurchaseDetailEntity detail = new PurchaseDetailEntity();
			detail.setPurchase(purchase);
			detail.setProduct(product);
			detail.setPresentation(presentation);
			detail.setQuantityPresentation(quantityPresentation);
			detail.setConversionFactor(conversionFactor);
			detail.setQuantityBase(quantityBase);
			detail.setUnitCostPresentation(unitCostPresentation);
			detail.setUnitCostBase(unitCostBase);
			detail.setSubtotal(subtotal);
			detail.setTaxAmount(taxAmount);
			detail.setTotalAmount(subtotal.add(taxAmount));
			detail.setExpirationDate(detailRequest.expirationDate());
			detail.setLotNumber(trimToNull(detailRequest.lotNumber()));
			purchaseDetailRepository.save(detail);
		}
	}

	private void recalculateTotals(PurchaseEntity purchase) {
		List<PurchaseDetailEntity> details = purchaseDetailRepository.findByPurchase_IdOrderByCreatedAtAsc(purchase.getId());
		BigDecimal subtotal = details.stream().map(PurchaseDetailEntity::getSubtotal).reduce(ZERO, BigDecimal::add);
		BigDecimal taxAmount = details.stream().map(PurchaseDetailEntity::getTaxAmount).reduce(ZERO, BigDecimal::add);
		BigDecimal discountAmount = nonNull(purchase.getDiscountAmount());
		BigDecimal totalAmount = subtotal.add(taxAmount).subtract(discountAmount);
		if (totalAmount.signum() < 0) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Discount cannot exceed purchase total");
		}

		purchase.setSubtotal(subtotal);
		purchase.setTaxAmount(taxAmount);
		purchase.setDiscountAmount(discountAmount);
		purchase.setTotalAmount(totalAmount);
	}

	private PurchaseResponse toResponse(PurchaseEntity purchase) {
		List<PurchaseDetailResponse> details = purchaseDetailRepository.findByPurchase_IdOrderByCreatedAtAsc(purchase.getId())
			.stream()
			.map(PurchaseDetailResponse::from)
			.toList();
		return PurchaseResponse.from(purchase, details, usernameMap(purchase.getCompany().getId(), List.of(purchase)));
	}

	private Map<UUID, String> usernameMap(UUID companyId, List<PurchaseEntity> purchases) {
		Set<UUID> userIds = new HashSet<>();
		for (PurchaseEntity purchase : purchases) {
			addIfNotNull(userIds, purchase.getCreatedBy());
			addIfNotNull(userIds, purchase.getConfirmedBy());
			addIfNotNull(userIds, purchase.getVoidedBy());
		}
		if (userIds.isEmpty()) {
			return Map.of();
		}
		return userRepository.findByCompany_IdAndIdInAndDeletedAtIsNull(companyId, userIds)
			.stream()
			.collect(java.util.stream.Collectors.toMap(UserEntity::getId, this::displayName));
	}

	private void addIfNotNull(Set<UUID> userIds, UUID userId) {
		if (userId != null) {
			userIds.add(userId);
		}
	}

	private String displayName(UserEntity user) {
		String name = (user.getFirstName() + " " + user.getLastName()).trim();
		return name.isBlank() ? user.getUsername() : name;
	}

	private String jsonStatus(PurchaseStatus status) {
		return "{\"status\":\"" + status.name() + "\"}";
	}

	private PurchaseEntity loadPurchase(UUID companyId, UUID purchaseId) {
		return purchaseRepository.findByIdAndCompany_Id(purchaseId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Purchase not found"));
	}

	private PurchaseEntity loadPurchase(AuthenticatedUser user, UUID purchaseId) {
		PurchaseEntity purchase = loadPurchase(user.companyId(), purchaseId);
		ensureCanAccessBranch(user, purchase.getBranch().getId());
		return purchase;
	}

	private PurchaseEntity loadPurchaseForUpdate(AuthenticatedUser user, UUID purchaseId) {
		PurchaseEntity purchase = purchaseRepository.findByIdAndCompanyIdForUpdate(purchaseId, user.companyId())
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Purchase not found"));
		ensureCanAccessBranch(user, purchase.getBranch().getId());
		return purchase;
	}

	private CompanyEntity loadCompany(UUID companyId) {
		return companyRepository.findByIdAndDeletedAtIsNull(companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Company not found"));
	}

	private String resolveCompanyTimezone(CompanyEntity company) {
		String timezone = company.getTimezone();
		return timezone == null || timezone.isBlank() ? "UTC" : timezone;
	}

	private BranchEntity loadBranch(AuthenticatedUser user, UUID branchId) {
		ensureCanAccessBranch(user, branchId);
		BranchEntity branch = branchRepository.findByIdAndCompany_IdAndDeletedAtIsNull(branchId, user.companyId())
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Branch not found"));
		if (!branch.isActive()) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Branch must be active");
		}
		return branch;
	}

	private UUID scopedBranchId(AuthenticatedUser user, UUID requestedBranchId) {
		if (canAccessAllBranches(user)) {
			return requestedBranchId;
		}
		if (requestedBranchId != null && !user.branchId().equals(requestedBranchId)) {
			throw ApiException.forbidden(ErrorCode.PERMISSION_DENIED, "User cannot access another branch");
		}
		return user.branchId();
	}

	private void ensureCanAccessBranch(AuthenticatedUser user, UUID branchId) {
		if (!canAccessAllBranches(user) && !user.branchId().equals(branchId)) {
			throw ApiException.forbidden(ErrorCode.PERMISSION_DENIED, "User cannot access another branch");
		}
	}

	private boolean canAccessAllBranches(AuthenticatedUser user) {
		return COMPANY_ADMIN.equals(user.roleCode());
	}

	private SupplierEntity loadSupplier(UUID companyId, UUID supplierId) {
		SupplierEntity supplier = supplierRepository.findByIdAndCompany_IdAndDeletedAtIsNull(supplierId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Supplier not found"));
		if (!supplier.isActive()) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Supplier must be active");
		}
		return supplier;
	}

	private PurchaseStatus normalizeWritableStatus(PurchaseStatus status) {
		PurchaseStatus normalizedStatus = status == null ? PurchaseStatus.DRAFT : status;
		if (normalizedStatus != PurchaseStatus.DRAFT && normalizedStatus != PurchaseStatus.PENDING_APPROVAL) {
			throw ApiException.badRequest(ErrorCode.INVALID_PURCHASE_STATUS, "Purchase can only be saved as draft or pending approval");
		}
		return normalizedStatus;
	}

	private void ensureWritable(PurchaseEntity purchase) {
		if (purchase.getStatus() == PurchaseStatus.CONFIRMED || purchase.getStatus() == PurchaseStatus.VOIDED) {
			throw ApiException.badRequest(ErrorCode.INVALID_PURCHASE_STATUS, "Confirmed or voided purchase cannot be edited");
		}
	}

	private void validatePurchaseNumberAvailable(UUID companyId, String purchaseNumber) {
		if (purchaseRepository.existsByCompany_IdAndPurchaseNumberIgnoreCase(companyId, purchaseNumber)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Purchase number already exists in the company");
		}
	}

	private String generatePurchaseNumber(UUID companyId) {
		String purchaseNumber;
		do {
			purchaseNumber = "PUR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
		} while (purchaseRepository.existsByCompany_IdAndPurchaseNumberIgnoreCase(companyId, purchaseNumber));
		return purchaseNumber;
	}

	private BigDecimal nonNull(BigDecimal value) {
		return value == null ? ZERO : value;
	}

	private String normalizeQuery(String query) {
		String trimmed = trimToNull(query);
		return trimmed == null ? "" : trimmed;
	}

	private Specification<PurchaseEntity> purchaseSearch(UUID companyId, UUID branchId, UUID supplierId, PurchaseStatus status, LocalDate fromDate, LocalDate toDate, String query) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			Predicate predicate = criteriaBuilder.equal(root.get("company").get("id"), companyId);
			if (branchId != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("branch").get("id"), branchId));
			}
			if (supplierId != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("supplier").get("id"), supplierId));
			}
			if (status != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
			}
			if (fromDate != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("purchaseDate"), fromDate));
			}
			if (toDate != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThanOrEqualTo(root.get("purchaseDate"), toDate));
			}
			if (!query.isBlank()) {
				String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
				Predicate queryPredicate = criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("purchaseNumber")), pattern),
					criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("supplierInvoiceNumber"), "")), pattern),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("supplier").get("name")), pattern)
				);
				predicate = criteriaBuilder.and(predicate, queryPredicate);
			}
			return predicate;
		};
	}

	private String normalizePurchaseNumber(String purchaseNumber) {
		String trimmed = trimToNull(purchaseNumber);
		return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
