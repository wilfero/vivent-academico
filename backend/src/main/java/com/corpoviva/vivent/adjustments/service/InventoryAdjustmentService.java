package com.corpoviva.vivent.adjustments.service;

import com.corpoviva.vivent.adjustments.dto.AdjustmentLineRequest;
import com.corpoviva.vivent.adjustments.dto.AdjustmentLineResponse;
import com.corpoviva.vivent.adjustments.dto.AdjustmentResponse;
import com.corpoviva.vivent.adjustments.dto.CancelAdjustmentRequest;
import com.corpoviva.vivent.adjustments.dto.RejectAdjustmentRequest;
import com.corpoviva.vivent.adjustments.dto.UpsertAdjustmentRequest;
import com.corpoviva.vivent.adjustments.entity.AdjustmentDirection;
import com.corpoviva.vivent.adjustments.entity.AdjustmentStatus;
import com.corpoviva.vivent.adjustments.entity.AdjustmentType;
import com.corpoviva.vivent.adjustments.entity.InventoryAdjustmentEntity;
import com.corpoviva.vivent.adjustments.entity.InventoryAdjustmentLineEntity;
import com.corpoviva.vivent.adjustments.repository.InventoryAdjustmentLineRepository;
import com.corpoviva.vivent.adjustments.repository.InventoryAdjustmentRepository;
import com.corpoviva.vivent.audit.entity.AuditAction;
import com.corpoviva.vivent.audit.service.AuditLogService;
import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.branches.entity.LocationEntity;
import com.corpoviva.vivent.branches.repository.BranchRepository;
import com.corpoviva.vivent.branches.repository.LocationRepository;
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
import com.corpoviva.vivent.products.service.ProductService;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.users.entity.UserEntity;
import com.corpoviva.vivent.users.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryAdjustmentService {

	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final String COMPANY_ADMIN = "COMPANY_ADMIN";
	private static final String ADJUSTMENT_LINE_SOURCE = "ADJUSTMENT_LINE";

	private final InventoryAdjustmentRepository adjustmentRepository;
	private final InventoryAdjustmentLineRepository lineRepository;
	private final CompanyRepository companyRepository;
	private final BranchRepository branchRepository;
	private final LocationRepository locationRepository;
	private final ProductService productService;
	private final ProductPresentationRepository presentationRepository;
	private final InventoryService inventoryService;
	private final AuditLogService auditLogService;
	private final UserRepository userRepository;

	public InventoryAdjustmentService(
		InventoryAdjustmentRepository adjustmentRepository,
		InventoryAdjustmentLineRepository lineRepository,
		CompanyRepository companyRepository,
		BranchRepository branchRepository,
		LocationRepository locationRepository,
		ProductService productService,
		ProductPresentationRepository presentationRepository,
		InventoryService inventoryService,
		AuditLogService auditLogService,
		UserRepository userRepository
	) {
		this.adjustmentRepository = adjustmentRepository;
		this.lineRepository = lineRepository;
		this.companyRepository = companyRepository;
		this.branchRepository = branchRepository;
		this.locationRepository = locationRepository;
		this.productService = productService;
		this.presentationRepository = presentationRepository;
		this.inventoryService = inventoryService;
		this.auditLogService = auditLogService;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<AdjustmentResponse> listAdjustments(AuthenticatedUser user, int page, int pageSize, UUID branchId, AdjustmentStatus status, Instant fromDate, Instant toDate, String query) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		UUID scopedBranchId = scopedBranchId(user, branchId);
		Page<InventoryAdjustmentEntity> adjustmentPage = adjustmentRepository.findAll(
			adjustmentSearch(user.companyId(), scopedBranchId, status, fromDate, toDate, normalizeQuery(query)),
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("createdAt").descending())
		);
		Map<UUID, String> usernames = usernameMap(user.companyId(), adjustmentPage.getContent());
		return PageResponse.from(adjustmentPage.map(adjustment -> AdjustmentResponse.from(adjustment, adjustmentLines(adjustment.getId()), usernames)), normalizedPage, normalizedPageSize);
	}

	@Transactional(readOnly = true)
	public AdjustmentResponse getAdjustment(AuthenticatedUser user, UUID adjustmentId) {
		return toResponse(loadAdjustment(user, adjustmentId));
	}

	@Transactional
	public AdjustmentResponse createAdjustment(AuthenticatedUser user, UpsertAdjustmentRequest request) {
		CompanyEntity company = loadCompany(user.companyId());
		BranchEntity branch = loadBranch(user, request.branchId() == null ? user.branchId() : request.branchId());
		LocationEntity location = loadLocation(user.companyId(), branch.getId(), request.locationId());
		AdjustmentStatus status = normalizeWritableStatus(request.status());
		String adjustmentNumber = normalizeAdjustmentNumber(request.adjustmentNumber());
		if (adjustmentNumber == null) {
			adjustmentNumber = generateAdjustmentNumber(user.companyId());
		} else {
			validateAdjustmentNumberAvailable(user.companyId(), adjustmentNumber);
		}

		InventoryAdjustmentEntity adjustment = new InventoryAdjustmentEntity();
		adjustment.setCompany(company);
		adjustment.setBranch(branch);
		adjustment.setLocation(location);
		adjustment.setAdjustmentNumber(adjustmentNumber);
		adjustment.setCreatedBy(user.userId());
		adjustment.setTimezoneSnapshot(resolveCompanyTimezone(company));
		applyAdjustmentFields(adjustment, request, status);
		InventoryAdjustmentEntity savedAdjustment = adjustmentRepository.save(adjustment);
		saveLines(user.companyId(), savedAdjustment, request.lines());
		auditLogService.log(user, savedAdjustment.getBranch().getId(), AuditAction.CREATE, "inventory_adjustment", savedAdjustment.getId(), null, jsonStatus(savedAdjustment.getStatus()));
		return toResponse(savedAdjustment);
	}

	@Transactional
	public AdjustmentResponse updateAdjustment(AuthenticatedUser user, UUID adjustmentId, UpsertAdjustmentRequest request) {
		InventoryAdjustmentEntity adjustment = loadAdjustmentForUpdate(user, adjustmentId);
		AdjustmentStatus oldStatus = adjustment.getStatus();
		ensureWritable(adjustment);
		BranchEntity branch = loadBranch(user, request.branchId() == null ? user.branchId() : request.branchId());
		LocationEntity location = loadLocation(user.companyId(), branch.getId(), request.locationId());
		AdjustmentStatus status = normalizeWritableStatus(request.status());
		String adjustmentNumber = normalizeAdjustmentNumber(request.adjustmentNumber());
		if (adjustmentNumber != null && !adjustmentNumber.equalsIgnoreCase(adjustment.getAdjustmentNumber())) {
			validateAdjustmentNumberAvailable(user.companyId(), adjustmentNumber);
			adjustment.setAdjustmentNumber(adjustmentNumber);
		}

		adjustment.setBranch(branch);
		adjustment.setLocation(location);
		applyAdjustmentFields(adjustment, request, status);
		lineRepository.deleteByAdjustment_Id(adjustment.getId());
		saveLines(user.companyId(), adjustment, request.lines());
		auditLogService.log(user, adjustment.getBranch().getId(), AuditAction.UPDATE, "inventory_adjustment", adjustment.getId(), jsonStatus(oldStatus), jsonStatus(adjustment.getStatus()));
		return toResponse(adjustment);
	}

	@Transactional
	public AdjustmentResponse approveAdjustment(AuthenticatedUser user, UUID adjustmentId) {
		InventoryAdjustmentEntity adjustment = loadAdjustmentForUpdate(user, adjustmentId);
		AdjustmentStatus oldStatus = adjustment.getStatus();
		if (adjustment.getStatus() != AdjustmentStatus.PENDING_APPROVAL && adjustment.getStatus() != AdjustmentStatus.DRAFT) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Only draft or pending adjustments can be approved");
		}

		List<InventoryAdjustmentLineEntity> lines = lineRepository.findByAdjustment_IdOrderByCreatedAtAsc(adjustment.getId());
		if (lines.isEmpty()) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Adjustment must have at least one line");
		}

		for (InventoryAdjustmentLineEntity line : lines) {
			if (adjustment.getDirection() == AdjustmentDirection.IN) {
				BigDecimal unitCost = line.getUnitCost() == null ? ZERO : line.getUnitCost();
				inventoryService.receiveStock(
					user,
					adjustment.getBranch().getId(),
					adjustment.getLocation().getId(),
					line.getProduct().getId(),
					line.getQuantityBase(),
					unitCost,
					null,
					null,
					movementTypeFor(adjustment),
					ADJUSTMENT_LINE_SOURCE,
					line.getId(),
					null,
					line.getNotes()
				);
			} else {
				inventoryService.issueStock(
					user,
					adjustment.getBranch().getId(),
					adjustment.getLocation().getId(),
					line.getProduct().getId(),
					line.getQuantityBase(),
					movementTypeFor(adjustment),
					ADJUSTMENT_LINE_SOURCE,
					line.getId(),
					line.getNotes()
				);
			}
		}

		adjustment.setStatus(AdjustmentStatus.APPROVED);
		adjustment.setApprovedBy(user.userId());
		adjustment.setApprovedAt(Instant.now());
		auditLogService.log(user, adjustment.getBranch().getId(), AuditAction.APPROVE, "inventory_adjustment", adjustment.getId(), jsonStatus(oldStatus), jsonStatus(adjustment.getStatus()));
		return toResponse(adjustment);
	}

	@Transactional
	public AdjustmentResponse rejectAdjustment(AuthenticatedUser user, UUID adjustmentId, RejectAdjustmentRequest request) {
		InventoryAdjustmentEntity adjustment = loadAdjustmentForUpdate(user, adjustmentId);
		AdjustmentStatus oldStatus = adjustment.getStatus();
		if (adjustment.getStatus() != AdjustmentStatus.PENDING_APPROVAL && adjustment.getStatus() != AdjustmentStatus.DRAFT) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Only draft or pending adjustments can be rejected");
		}
		String reason = trimToNull(request.reason());
		adjustment.setStatus(AdjustmentStatus.REJECTED);
		adjustment.setRejectedBy(user.userId());
		adjustment.setRejectedAt(Instant.now());
		adjustment.setRejectionReason(reason);
		auditLogService.log(user, adjustment.getBranch().getId(), AuditAction.REJECT, "inventory_adjustment", adjustment.getId(), jsonStatus(oldStatus), jsonStatus(adjustment.getStatus()));
		return toResponse(adjustment);
	}

	@Transactional
	public AdjustmentResponse cancelAdjustment(AuthenticatedUser user, UUID adjustmentId, CancelAdjustmentRequest request) {
		InventoryAdjustmentEntity adjustment = loadAdjustmentForUpdate(user, adjustmentId);
		AdjustmentStatus oldStatus = adjustment.getStatus();
		if (adjustment.getStatus() == AdjustmentStatus.APPROVED || adjustment.getStatus() == AdjustmentStatus.CANCELLED) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Adjustment cannot be cancelled");
		}
		adjustment.setStatus(AdjustmentStatus.CANCELLED);
		adjustment.setRejectionReason(trimToNull(request.reason()));
		auditLogService.log(user, adjustment.getBranch().getId(), AuditAction.CANCEL, "inventory_adjustment", adjustment.getId(), jsonStatus(oldStatus), jsonStatus(adjustment.getStatus()));
		return toResponse(adjustment);
	}

	private void applyAdjustmentFields(InventoryAdjustmentEntity adjustment, UpsertAdjustmentRequest request, AdjustmentStatus status) {
		adjustment.setAdjustmentType(request.adjustmentType());
		adjustment.setDirection(request.direction());
		adjustment.setStatus(status);
		adjustment.setNotes(trimToNull(request.notes()));
	}

	private void saveLines(UUID companyId, InventoryAdjustmentEntity adjustment, List<AdjustmentLineRequest> lineRequests) {
		for (AdjustmentLineRequest lineRequest : lineRequests) {
			ProductPresentationEntity presentation = null;
			ProductEntity product;
			BigDecimal quantityPresentation = lineRequest.quantityPresentation();
			BigDecimal conversionFactor = null;
			BigDecimal quantityBase = lineRequest.quantityBase();

			if (lineRequest.presentationId() != null) {
				presentation = presentationRepository.findByIdAndCompany_IdAndDeletedAtIsNull(lineRequest.presentationId(), companyId)
					.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Presentation not found"));
				product = presentation.getProduct();
				conversionFactor = presentation.getConversionFactor();
				if (quantityPresentation == null) {
					throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Quantity presentation is required when presentation is provided");
				}
				quantityBase = quantityPresentation.multiply(conversionFactor);
			} else {
				if (lineRequest.productId() == null || quantityBase == null) {
					throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Product and base quantity are required without presentation");
				}
				product = productService.loadProduct(companyId, lineRequest.productId());
			}

			if (!product.isActive() || !product.isStockable()) {
				throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Product must be active and stockable");
			}

			InventoryAdjustmentLineEntity line = new InventoryAdjustmentLineEntity();
			line.setAdjustment(adjustment);
			line.setProduct(product);
			line.setPresentation(presentation);
			line.setQuantityPresentation(quantityPresentation);
			line.setConversionFactor(conversionFactor);
			line.setQuantityBase(quantityBase);
			line.setUnitCost(lineRequest.unitCost());
			line.setNotes(trimToNull(lineRequest.notes()));
			lineRepository.save(line);
		}
	}

	private StockMovementType movementTypeFor(InventoryAdjustmentEntity adjustment) {
		if (adjustment.getDirection() == AdjustmentDirection.IN) {
			return adjustment.getAdjustmentType() == AdjustmentType.PHYSICAL_COUNT ? StockMovementType.STOCK_COUNT_IN : StockMovementType.ADJUSTMENT_IN;
		}
		return switch (adjustment.getAdjustmentType()) {
			case PHYSICAL_COUNT -> StockMovementType.STOCK_COUNT_OUT;
			case DAMAGE -> StockMovementType.DAMAGE_OUT;
			case LOSS, THEFT -> StockMovementType.LOSS_OUT;
			case EXPIRED -> StockMovementType.EXPIRED_OUT;
			default -> StockMovementType.ADJUSTMENT_OUT;
		};
	}

	private AdjustmentResponse toResponse(InventoryAdjustmentEntity adjustment) {
		return AdjustmentResponse.from(adjustment, adjustmentLines(adjustment.getId()), usernameMap(adjustment.getCompany().getId(), List.of(adjustment)));
	}

	private List<AdjustmentLineResponse> adjustmentLines(UUID adjustmentId) {
		return lineRepository.findByAdjustment_IdOrderByCreatedAtAsc(adjustmentId)
			.stream()
			.map(AdjustmentLineResponse::from)
			.toList();
	}

	private Map<UUID, String> usernameMap(UUID companyId, List<InventoryAdjustmentEntity> adjustments) {
		Set<UUID> userIds = new HashSet<>();
		for (InventoryAdjustmentEntity adjustment : adjustments) {
			addIfNotNull(userIds, adjustment.getCreatedBy());
			addIfNotNull(userIds, adjustment.getApprovedBy());
			addIfNotNull(userIds, adjustment.getRejectedBy());
		}
		if (userIds.isEmpty()) {
			return Map.of();
		}
		return userRepository.findByCompany_IdAndIdInAndDeletedAtIsNull(companyId, userIds)
			.stream()
			.collect(Collectors.toMap(UserEntity::getId, this::displayName));
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

	private InventoryAdjustmentEntity loadAdjustment(UUID companyId, UUID adjustmentId) {
		return adjustmentRepository.findByIdAndCompany_Id(adjustmentId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Adjustment not found"));
	}

	private InventoryAdjustmentEntity loadAdjustment(AuthenticatedUser user, UUID adjustmentId) {
		InventoryAdjustmentEntity adjustment = loadAdjustment(user.companyId(), adjustmentId);
		ensureCanAccessBranch(user, adjustment.getBranch().getId());
		return adjustment;
	}

	private InventoryAdjustmentEntity loadAdjustmentForUpdate(AuthenticatedUser user, UUID adjustmentId) {
		InventoryAdjustmentEntity adjustment = adjustmentRepository.findByIdAndCompanyIdForUpdate(adjustmentId, user.companyId())
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Adjustment not found"));
		ensureCanAccessBranch(user, adjustment.getBranch().getId());
		return adjustment;
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

	private LocationEntity loadLocation(UUID companyId, UUID branchId, UUID locationId) {
		if (locationId == null) {
			return locationRepository.findByCompany_IdAndBranch_IdAndDefaultLocationTrueAndActiveTrue(companyId, branchId)
				.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Default location not found"));
		}
		return locationRepository.findByIdAndCompany_IdAndBranch_Id(locationId, companyId, branchId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Location not found"));
	}

	private AdjustmentStatus normalizeWritableStatus(AdjustmentStatus status) {
		AdjustmentStatus normalizedStatus = status == null ? AdjustmentStatus.DRAFT : status;
		if (normalizedStatus != AdjustmentStatus.DRAFT && normalizedStatus != AdjustmentStatus.PENDING_APPROVAL) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Adjustment can only be saved as draft or pending approval");
		}
		return normalizedStatus;
	}

	private void ensureWritable(InventoryAdjustmentEntity adjustment) {
		if (adjustment.getStatus() == AdjustmentStatus.APPROVED || adjustment.getStatus() == AdjustmentStatus.REJECTED || adjustment.getStatus() == AdjustmentStatus.CANCELLED) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Final adjustment cannot be edited");
		}
	}

	private void validateAdjustmentNumberAvailable(UUID companyId, String adjustmentNumber) {
		if (adjustmentRepository.existsByCompany_IdAndAdjustmentNumberIgnoreCase(companyId, adjustmentNumber)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Adjustment number already exists in the company");
		}
	}

	private String generateAdjustmentNumber(UUID companyId) {
		String adjustmentNumber;
		do {
			adjustmentNumber = "ADJ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
		} while (adjustmentRepository.existsByCompany_IdAndAdjustmentNumberIgnoreCase(companyId, adjustmentNumber));
		return adjustmentNumber;
	}

	private String jsonStatus(AdjustmentStatus status) {
		return "{\"status\":\"" + status.name() + "\"}";
	}

	private String normalizeQuery(String query) {
		String trimmed = trimToNull(query);
		return trimmed == null ? "" : trimmed;
	}

	private String normalizeAdjustmentNumber(String adjustmentNumber) {
		String trimmed = trimToNull(adjustmentNumber);
		return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
	}

	private Specification<InventoryAdjustmentEntity> adjustmentSearch(UUID companyId, UUID branchId, AdjustmentStatus status, Instant fromDate, Instant toDate, String query) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			Predicate predicate = criteriaBuilder.equal(root.get("company").get("id"), companyId);
			if (branchId != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("branch").get("id"), branchId));
			}
			if (status != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
			}
			if (fromDate != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
			}
			if (toDate != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate));
			}
			if (!query.isBlank()) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(criteriaBuilder.lower(root.get("adjustmentNumber")), "%" + query.toLowerCase(Locale.ROOT) + "%"));
			}
			return predicate;
		};
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
