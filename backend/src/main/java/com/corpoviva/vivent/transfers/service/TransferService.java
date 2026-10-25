package com.corpoviva.vivent.transfers.service;

import com.corpoviva.vivent.audit.entity.AuditAction;
import com.corpoviva.vivent.audit.service.AuditLogService;
import com.corpoviva.vivent.branches.dto.BranchResponse;
import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.branches.repository.BranchRepository;
import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.inventory.dto.StockOperationResponse;
import com.corpoviva.vivent.inventory.entity.StockMovementType;
import com.corpoviva.vivent.inventory.service.InventoryService;
import com.corpoviva.vivent.products.entity.ProductEntity;
import com.corpoviva.vivent.products.entity.ProductPresentationEntity;
import com.corpoviva.vivent.products.repository.ProductPresentationRepository;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.transfers.dto.CancelTransferRequest;
import com.corpoviva.vivent.transfers.dto.CreateTransferRequest;
import com.corpoviva.vivent.transfers.dto.ReceiveTransferDetailRequest;
import com.corpoviva.vivent.transfers.dto.ReceiveTransferRequest;
import com.corpoviva.vivent.transfers.dto.TransferDetailRequest;
import com.corpoviva.vivent.transfers.dto.TransferDetailResponse;
import com.corpoviva.vivent.transfers.dto.TransferResponse;
import com.corpoviva.vivent.transfers.entity.TransferDetailEntity;
import com.corpoviva.vivent.transfers.entity.TransferEntity;
import com.corpoviva.vivent.transfers.entity.TransferStatus;
import com.corpoviva.vivent.transfers.repository.TransferDetailRepository;
import com.corpoviva.vivent.transfers.repository.TransferRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final String TRANSFER_DETAIL_SOURCE = "TRANSFER_DETAIL";
	private static final String TRANSFER_RECEIVE_SOURCE = "TRANSFER_RECEIVE";

	private final TransferRepository transferRepository;
	private final TransferDetailRepository transferDetailRepository;
	private final CompanyRepository companyRepository;
	private final BranchRepository branchRepository;
	private final ProductPresentationRepository productPresentationRepository;
	private final InventoryService inventoryService;
	private final AuditLogService auditLogService;
	private final UserRepository userRepository;

	public TransferService(
		TransferRepository transferRepository,
		TransferDetailRepository transferDetailRepository,
		CompanyRepository companyRepository,
		BranchRepository branchRepository,
		ProductPresentationRepository productPresentationRepository,
		InventoryService inventoryService,
		AuditLogService auditLogService,
		UserRepository userRepository
	) {
		this.transferRepository = transferRepository;
		this.transferDetailRepository = transferDetailRepository;
		this.companyRepository = companyRepository;
		this.branchRepository = branchRepository;
		this.productPresentationRepository = productPresentationRepository;
		this.inventoryService = inventoryService;
		this.auditLogService = auditLogService;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<BranchResponse> listSourceBranches(AuthenticatedUser user, int page, int pageSize, String query) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		Page<BranchEntity> branchPage = branchRepository.searchByCompany(
			user.companyId(),
			normalizeQuery(query),
			true,
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("name").ascending())
		);

		return PageResponse.from(branchPage.map(BranchResponse::from), normalizedPage, normalizedPageSize);
	}

	@Transactional(readOnly = true)
	public PageResponse<TransferResponse> listTransfers(
		AuthenticatedUser user,
		int page,
		int pageSize,
		UUID sourceBranchId,
		UUID destinationBranchId,
		TransferStatus status,
		Instant fromDate,
		Instant toDate,
		String query
	) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		Page<TransferEntity> transferPage = transferRepository.findAll(
			transferSearch(user.companyId(), sourceBranchId, destinationBranchId, status, fromDate, toDate, normalizeQuery(query), canAccessAllBranches(user) ? null : user.branchId()),
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("createdAt").descending())
		);

		Map<UUID, String> usernames = usernameMap(user.companyId(), transferPage.getContent());
		return PageResponse.from(transferPage.map(transfer -> TransferResponse.from(transfer, transferDetails(transfer.getId()), usernames)), normalizedPage, normalizedPageSize);
	}

	@Transactional(readOnly = true)
	public TransferResponse getTransfer(AuthenticatedUser user, UUID transferId) {
		TransferEntity transfer = loadTransfer(user.companyId(), transferId);
		ensureCanViewTransfer(user, transfer);
		return toResponse(user.companyId(), transfer);
	}

	@Transactional
	public TransferResponse createTransfer(AuthenticatedUser user, CreateTransferRequest request) {
		CompanyEntity company = loadCompany(user.companyId());
		BranchEntity sourceBranch = loadBranch(user.companyId(), request.sourceBranchId());
		BranchEntity destinationBranch = loadBranch(user.companyId(), request.destinationBranchId());
		if (!canAccessAllBranches(user) && !destinationBranch.getId().equals(user.branchId())) {
			throw ApiException.forbidden(ErrorCode.PERMISSION_DENIED, "Transfer destination must be your assigned branch");
		}
		if (sourceBranch.getId().equals(destinationBranch.getId())) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Transfer branches must be different");
		}

		String transferNumber = normalizeTransferNumber(request.transferNumber());
		if (transferNumber == null) {
			transferNumber = generateTransferNumber(user.companyId());
		} else {
			validateTransferNumberAvailable(user.companyId(), transferNumber);
		}

		TransferEntity transfer = new TransferEntity();
		transfer.setCompany(company);
		transfer.setSourceBranch(sourceBranch);
		transfer.setDestinationBranch(destinationBranch);
		transfer.setTransferNumber(transferNumber);
		transfer.setStatus(TransferStatus.REQUESTED);
		transfer.setRequestedBy(user.userId());
		transfer.setRequestedAt(Instant.now());
		transfer.setTimezoneSnapshot(resolveCompanyTimezone(company));
		transfer.setNotes(trimToNull(request.notes()));
		TransferEntity savedTransfer = transferRepository.save(transfer);

		saveDetails(user.companyId(), savedTransfer, request.details());
		return toResponse(user.companyId(), savedTransfer);
	}

	@Transactional
	public TransferResponse approveTransfer(AuthenticatedUser user, UUID transferId) {
		TransferEntity transfer = loadTransferForUpdate(user.companyId(), transferId);
		TransferStatus oldStatus = transfer.getStatus();
		ensureCanOperateSource(user, transfer);
		if (transfer.getStatus() != TransferStatus.REQUESTED) {
			throw ApiException.badRequest(ErrorCode.INVALID_TRANSFER_STATUS, "Only requested transfers can be approved");
		}

		List<TransferDetailEntity> details = transferDetailRepository.findByTransfer_IdOrderByCreatedAtAsc(transfer.getId());
		if (details.isEmpty()) {
			throw ApiException.badRequest(ErrorCode.INVALID_TRANSFER_STATUS, "Transfer must have at least one detail");
		}

		for (TransferDetailEntity detail : details) {
			detail.setQuantityApprovedBase(detail.getQuantityRequestedBase());
			inventoryService.reserveStock(
				user,
				transfer.getSourceBranch().getId(),
				null,
				detail.getProduct().getId(),
				detail.getQuantityApprovedBase(),
				TRANSFER_DETAIL_SOURCE,
				detail.getId(),
				"Transfer " + transfer.getTransferNumber()
			);
		}

		transfer.setStatus(TransferStatus.APPROVED);
		transfer.setApprovedBy(user.userId());
		transfer.setApprovedAt(Instant.now());
		auditLogService.log(user, transfer.getSourceBranch().getId(), AuditAction.APPROVE, "transfer", transfer.getId(), jsonStatus(oldStatus), jsonStatus(transfer.getStatus()));
		return toResponse(user.companyId(), transfer);
	}

	@Transactional
	public TransferResponse rejectTransfer(AuthenticatedUser user, UUID transferId) {
		TransferEntity transfer = loadTransferForUpdate(user.companyId(), transferId);
		TransferStatus oldStatus = transfer.getStatus();
		ensureCanOperateSource(user, transfer);
		if (transfer.getStatus() != TransferStatus.REQUESTED) {
			throw ApiException.badRequest(ErrorCode.INVALID_TRANSFER_STATUS, "Only requested transfers can be rejected");
		}
		transfer.setStatus(TransferStatus.REJECTED);
		transfer.setCancelledBy(user.userId());
		transfer.setCancelledAt(Instant.now());
		auditLogService.log(user, transfer.getSourceBranch().getId(), AuditAction.REJECT, "transfer", transfer.getId(), jsonStatus(oldStatus), jsonStatus(transfer.getStatus()));
		return toResponse(user.companyId(), transfer);
	}

	@Transactional
	public TransferResponse dispatchTransfer(AuthenticatedUser user, UUID transferId) {
		TransferEntity transfer = loadTransferForUpdate(user.companyId(), transferId);
		TransferStatus oldStatus = transfer.getStatus();
		ensureCanOperateSource(user, transfer);
		if (transfer.getStatus() != TransferStatus.APPROVED) {
			throw ApiException.badRequest(ErrorCode.INVALID_TRANSFER_STATUS, "Only approved transfers can be dispatched");
		}
		transfer.setStatus(TransferStatus.IN_TRANSIT);
		transfer.setDispatchedBy(user.userId());
		transfer.setDispatchedAt(Instant.now());
		auditLogService.log(user, transfer.getSourceBranch().getId(), AuditAction.DISPATCH, "transfer", transfer.getId(), jsonStatus(oldStatus), jsonStatus(transfer.getStatus()));
		return toResponse(user.companyId(), transfer);
	}

	@Transactional
	public TransferResponse receiveTransfer(AuthenticatedUser user, UUID transferId, ReceiveTransferRequest request) {
		TransferEntity transfer = loadTransferForUpdate(user.companyId(), transferId);
		TransferStatus oldStatus = transfer.getStatus();
		ensureCanOperateDestination(user, transfer);
		if (transfer.getStatus() != TransferStatus.APPROVED
			&& transfer.getStatus() != TransferStatus.IN_TRANSIT
			&& transfer.getStatus() != TransferStatus.PARTIALLY_RECEIVED) {
			throw ApiException.badRequest(ErrorCode.INVALID_TRANSFER_STATUS, "Transfer is not receivable");
		}

		List<TransferDetailEntity> details = transferDetailRepository.findByTransfer_IdOrderByCreatedAtAsc(transfer.getId());
		Map<UUID, TransferDetailEntity> detailById = details.stream().collect(Collectors.toMap(TransferDetailEntity::getId, Function.identity()));
		boolean receivedAnyQuantity = false;
		for (ReceiveTransferDetailRequest receivedDetail : request.details()) {
			TransferDetailEntity detail = detailById.get(receivedDetail.detailId());
			if (detail == null) {
				throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Transfer detail not found in transfer");
			}

			BigDecimal quantity = receivedDetail.quantityReceivedBase();
			if (quantity.signum() == 0) {
				continue;
			}
			receivedAnyQuantity = true;

			BigDecimal remaining = detail.getQuantityApprovedBase().subtract(detail.getQuantityReceivedBase());
			if (quantity.compareTo(remaining) > 0) {
				throw ApiException.badRequest(ErrorCode.INVALID_TRANSFER_STATUS, "Cannot receive more than approved quantity");
			}

			inventoryService.releaseReservedStock(user, transfer.getSourceBranch().getId(), null, detail.getProduct().getId(), quantity);
			StockOperationResponse issueOperation = inventoryService.issueStock(
				user,
				transfer.getSourceBranch().getId(),
				null,
				detail.getProduct().getId(),
				quantity,
				StockMovementType.TRANSFER_OUT,
				TRANSFER_DETAIL_SOURCE,
				detail.getId(),
				"Transfer " + transfer.getTransferNumber()
			);
			inventoryService.receiveStock(
				user,
				transfer.getDestinationBranch().getId(),
				null,
				detail.getProduct().getId(),
				quantity,
				issueOperation.movement().unitCost(),
				null,
				null,
				StockMovementType.TRANSFER_IN,
				TRANSFER_RECEIVE_SOURCE,
				detail.getId(),
				null,
				"Transfer " + transfer.getTransferNumber()
			);
			detail.setQuantityReceivedBase(detail.getQuantityReceivedBase().add(quantity));
		}
		if (!receivedAnyQuantity) {
			throw ApiException.badRequest(ErrorCode.INVALID_TRANSFER_STATUS, "At least one received quantity is required");
		}

		boolean allReceived = details.stream().allMatch(detail -> detail.getQuantityReceivedBase().compareTo(detail.getQuantityApprovedBase()) == 0);
		boolean anyReceived = details.stream().anyMatch(detail -> detail.getQuantityReceivedBase().signum() > 0);
		transfer.setStatus(allReceived ? TransferStatus.RECEIVED : TransferStatus.PARTIALLY_RECEIVED);
		if (anyReceived) {
			transfer.setReceivedBy(user.userId());
			transfer.setReceivedAt(Instant.now());
		}
		auditLogService.log(user, transfer.getDestinationBranch().getId(), AuditAction.RECEIVE, "transfer", transfer.getId(), jsonStatus(oldStatus), jsonStatus(transfer.getStatus()));
		return toResponse(user.companyId(), transfer);
	}

	@Transactional
	public TransferResponse cancelTransfer(AuthenticatedUser user, UUID transferId, CancelTransferRequest request) {
		TransferEntity transfer = loadTransferForUpdate(user.companyId(), transferId);
		TransferStatus oldStatus = transfer.getStatus();
		ensureCanViewTransfer(user, transfer);
		if (transfer.getStatus() == TransferStatus.CANCELLED || transfer.getStatus() == TransferStatus.RECEIVED || transfer.getStatus() == TransferStatus.REJECTED) {
			throw ApiException.badRequest(ErrorCode.INVALID_TRANSFER_STATUS, "Transfer cannot be cancelled");
		}

		String reason = trimToNull(request.reason());
		if (reason == null) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Cancel reason is required");
		}

		if (transfer.getStatus() == TransferStatus.APPROVED
			|| transfer.getStatus() == TransferStatus.IN_TRANSIT
			|| transfer.getStatus() == TransferStatus.PARTIALLY_RECEIVED) {
			List<TransferDetailEntity> details = transferDetailRepository.findByTransfer_IdOrderByCreatedAtAsc(transfer.getId());
			for (TransferDetailEntity detail : details) {
				BigDecimal reservedRemaining = detail.getQuantityApprovedBase().subtract(detail.getQuantityReceivedBase());
				if (reservedRemaining.signum() > 0) {
					inventoryService.releaseReservedStock(user, transfer.getSourceBranch().getId(), null, detail.getProduct().getId(), reservedRemaining);
				}
			}
		}

		transfer.setStatus(TransferStatus.CANCELLED);
		transfer.setCancelledBy(user.userId());
		transfer.setCancelledAt(Instant.now());
		transfer.setCancelReason(reason);
		auditLogService.log(user, auditBranchFor(user, transfer), AuditAction.CANCEL, "transfer", transfer.getId(), jsonStatus(oldStatus), jsonStatus(transfer.getStatus()));
		return toResponse(user.companyId(), transfer);
	}

	private void saveDetails(UUID companyId, TransferEntity transfer, List<TransferDetailRequest> detailRequests) {
		for (TransferDetailRequest detailRequest : detailRequests) {
			ProductPresentationEntity presentation = productPresentationRepository
				.findByIdAndCompany_IdAndDeletedAtIsNull(detailRequest.presentationId(), companyId)
				.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Presentation not found"));
			ProductEntity product = presentation.getProduct();
			if (!presentation.isActive() || !product.isActive() || !product.isStockable()) {
				throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Presentation product must be active and stockable");
			}

			TransferDetailEntity detail = new TransferDetailEntity();
			detail.setTransfer(transfer);
			detail.setProduct(product);
			detail.setPresentation(presentation);
			detail.setQuantityRequestedPresentation(detailRequest.quantityRequestedPresentation());
			detail.setConversionFactor(presentation.getConversionFactor());
			detail.setQuantityRequestedBase(detailRequest.quantityRequestedPresentation().multiply(presentation.getConversionFactor()));
			detail.setQuantityApprovedBase(ZERO);
			detail.setQuantityReceivedBase(ZERO);
			transferDetailRepository.save(detail);
		}
	}

	private TransferResponse toResponse(UUID companyId, TransferEntity transfer) {
		return TransferResponse.from(transfer, transferDetails(transfer.getId()), usernameMap(companyId, List.of(transfer)));
	}

	private List<TransferDetailResponse> transferDetails(UUID transferId) {
		return transferDetailRepository.findByTransfer_IdOrderByCreatedAtAsc(transferId)
			.stream()
			.map(TransferDetailResponse::from)
			.toList();
	}

	private Map<UUID, String> usernameMap(UUID companyId, List<TransferEntity> transfers) {
		Set<UUID> userIds = new HashSet<>();
		for (TransferEntity transfer : transfers) {
			addIfNotNull(userIds, transfer.getRequestedBy());
			addIfNotNull(userIds, transfer.getApprovedBy());
			addIfNotNull(userIds, transfer.getDispatchedBy());
			addIfNotNull(userIds, transfer.getReceivedBy());
			addIfNotNull(userIds, transfer.getCancelledBy());
		}
		return userRepository.findByCompany_IdAndIdInAndDeletedAtIsNull(companyId, userIds).stream().collect(Collectors.toMap(UserEntity::getId, this::displayName));
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

	private UUID auditBranchFor(AuthenticatedUser user, TransferEntity transfer) {
		return canAccessAllBranches(user) ? transfer.getSourceBranch().getId() : user.branchId();
	}

	private String jsonStatus(TransferStatus status) {
		return "{\"status\":\"" + status.name() + "\"}";
	}

	private void ensureCanViewTransfer(AuthenticatedUser user, TransferEntity transfer) {
		if (canAccessAllBranches(user)) {
			return;
		}
		if (transfer.getSourceBranch().getId().equals(user.branchId()) || transfer.getDestinationBranch().getId().equals(user.branchId())) {
			return;
		}
		throw ApiException.forbidden(ErrorCode.PERMISSION_DENIED, "User cannot access this transfer");
	}

	private void ensureCanOperateSource(AuthenticatedUser user, TransferEntity transfer) {
		if (canAccessAllBranches(user) || transfer.getSourceBranch().getId().equals(user.branchId())) {
			return;
		}
		throw ApiException.forbidden(ErrorCode.PERMISSION_DENIED, "Only source branch can perform this action");
	}

	private void ensureCanOperateDestination(AuthenticatedUser user, TransferEntity transfer) {
		if (canAccessAllBranches(user) || transfer.getDestinationBranch().getId().equals(user.branchId())) {
			return;
		}
		throw ApiException.forbidden(ErrorCode.PERMISSION_DENIED, "Only destination branch can receive this transfer");
	}

	private boolean canAccessAllBranches(AuthenticatedUser user) {
		return "COMPANY_ADMIN".equals(user.roleCode());
	}

	private TransferEntity loadTransfer(UUID companyId, UUID transferId) {
		return transferRepository.findByIdAndCompany_Id(transferId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Transfer not found"));
	}

	private TransferEntity loadTransferForUpdate(UUID companyId, UUID transferId) {
		return transferRepository.findByIdAndCompanyIdForUpdate(transferId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Transfer not found"));
	}

	private CompanyEntity loadCompany(UUID companyId) {
		return companyRepository.findByIdAndDeletedAtIsNull(companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Company not found"));
	}

	private String resolveCompanyTimezone(CompanyEntity company) {
		String timezone = company.getTimezone();
		return timezone == null || timezone.isBlank() ? "UTC" : timezone;
	}

	private BranchEntity loadBranch(UUID companyId, UUID branchId) {
		BranchEntity branch = branchRepository.findByIdAndCompany_IdAndDeletedAtIsNull(branchId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Branch not found"));
		if (!branch.isActive()) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Branch must be active");
		}
		return branch;
	}

	private void validateTransferNumberAvailable(UUID companyId, String transferNumber) {
		if (transferRepository.existsByCompany_IdAndTransferNumberIgnoreCase(companyId, transferNumber)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Transfer number already exists in the company");
		}
	}

	private String generateTransferNumber(UUID companyId) {
		String transferNumber;
		do {
			transferNumber = "TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
		} while (transferRepository.existsByCompany_IdAndTransferNumberIgnoreCase(companyId, transferNumber));
		return transferNumber;
	}

	private String normalizeQuery(String query) {
		String trimmed = trimToNull(query);
		return trimmed == null ? "" : trimmed;
	}

	private String normalizeTransferNumber(String transferNumber) {
		String trimmed = trimToNull(transferNumber);
		return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
	}

	private Specification<TransferEntity> transferSearch(UUID companyId, UUID sourceBranchId, UUID destinationBranchId, TransferStatus status, Instant fromDate, Instant toDate, String query, UUID visibleBranchId) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			Predicate predicate = criteriaBuilder.equal(root.get("company").get("id"), companyId);
			if (sourceBranchId != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("sourceBranch").get("id"), sourceBranchId));
			}
			if (destinationBranchId != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("destinationBranch").get("id"), destinationBranchId));
			}
			if (status != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
			}
			if (fromDate != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("requestedAt"), fromDate));
			}
			if (toDate != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThanOrEqualTo(root.get("requestedAt"), toDate));
			}
			if (!query.isBlank()) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(criteriaBuilder.lower(root.get("transferNumber")), "%" + query.toLowerCase(Locale.ROOT) + "%"));
			}
			if (visibleBranchId != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.or(
					criteriaBuilder.equal(root.get("sourceBranch").get("id"), visibleBranchId),
					criteriaBuilder.equal(root.get("destinationBranch").get("id"), visibleBranchId)
				));
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
