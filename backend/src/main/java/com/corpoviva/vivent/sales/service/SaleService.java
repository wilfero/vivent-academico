package com.corpoviva.vivent.sales.service;

import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.branches.repository.BranchRepository;
import com.corpoviva.vivent.audit.entity.AuditAction;
import com.corpoviva.vivent.audit.service.AuditLogService;
import com.corpoviva.vivent.combos.entity.ComboOfferEntity;
import com.corpoviva.vivent.combos.entity.ComboOfferItemEntity;
import com.corpoviva.vivent.combos.entity.SaleComboLineEntity;
import com.corpoviva.vivent.combos.repository.ComboOfferRepository;
import com.corpoviva.vivent.combos.repository.SaleComboLineRepository;
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
import com.corpoviva.vivent.sales.dto.CancelSaleRequest;
import com.corpoviva.vivent.sales.dto.SaleComboLineRequest;
import com.corpoviva.vivent.sales.dto.SaleDetailRequest;
import com.corpoviva.vivent.sales.dto.SaleDetailResponse;
import com.corpoviva.vivent.sales.dto.SaleResponse;
import com.corpoviva.vivent.sales.dto.UpsertSaleRequest;
import com.corpoviva.vivent.sales.entity.CustomerEntity;
import com.corpoviva.vivent.sales.entity.SaleDetailEntity;
import com.corpoviva.vivent.sales.entity.SaleDetailSourceType;
import com.corpoviva.vivent.sales.entity.SaleEntity;
import com.corpoviva.vivent.sales.entity.SalePaymentMethod;
import com.corpoviva.vivent.sales.entity.SaleStatus;
import com.corpoviva.vivent.sales.repository.CustomerRepository;
import com.corpoviva.vivent.sales.repository.SaleDetailRepository;
import com.corpoviva.vivent.sales.repository.SaleRepository;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.users.entity.UserEntity;
import com.corpoviva.vivent.users.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {

	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final String COMPANY_ADMIN = "COMPANY_ADMIN";
	private static final String SALE_DETAIL_SOURCE = "SALE_DETAIL";
	private static final String SALE_CANCEL_SOURCE = "SALE_CANCEL";

	private final SaleRepository saleRepository;
	private final SaleDetailRepository saleDetailRepository;
	private final CustomerRepository customerRepository;
	private final CompanyRepository companyRepository;
	private final BranchRepository branchRepository;
	private final ProductPresentationRepository productPresentationRepository;
	private final InventoryService inventoryService;
	private final AuditLogService auditLogService;
	private final UserRepository userRepository;
	private final ComboOfferRepository comboOfferRepository;
	private final SaleComboLineRepository saleComboLineRepository;

	public SaleService(
		SaleRepository saleRepository,
		SaleDetailRepository saleDetailRepository,
		CustomerRepository customerRepository,
		CompanyRepository companyRepository,
		BranchRepository branchRepository,
		ProductPresentationRepository productPresentationRepository,
		InventoryService inventoryService,
		AuditLogService auditLogService,
		UserRepository userRepository,
		ComboOfferRepository comboOfferRepository,
		SaleComboLineRepository saleComboLineRepository
	) {
		this.saleRepository = saleRepository;
		this.saleDetailRepository = saleDetailRepository;
		this.customerRepository = customerRepository;
		this.companyRepository = companyRepository;
		this.branchRepository = branchRepository;
		this.productPresentationRepository = productPresentationRepository;
		this.inventoryService = inventoryService;
		this.auditLogService = auditLogService;
		this.userRepository = userRepository;
		this.comboOfferRepository = comboOfferRepository;
		this.saleComboLineRepository = saleComboLineRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<SaleResponse> listSales(
		AuthenticatedUser user,
		int page,
		int pageSize,
		UUID branchId,
		UUID customerId,
		SaleStatus status,
		SalePaymentMethod paymentMethod,
		Instant fromDate,
		Instant toDate,
		String query
	) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		UUID scopedBranchId = resolveScopedBranchId(user, branchId);
		Page<SaleEntity> salePage = saleRepository.findAll(
			saleSearch(user.companyId(), scopedBranchId, customerId, status, paymentMethod, fromDate, toDate, normalizeQuery(query)),
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("createdAt").descending())
		);

		Map<UUID, String> usernames = usernameMap(user.companyId(), salePage.getContent());
		List<UUID> saleIds = salePage.getContent().stream().map(SaleEntity::getId).toList();
		Map<UUID, List<SaleComboLineEntity>> comboLinesBySaleId = saleIds.isEmpty()
			? Map.of()
			: saleComboLineRepository.findBySale_IdInOrderByCreatedAtAsc(saleIds).stream()
				.collect(Collectors.groupingBy(comboLine -> comboLine.getSale().getId()));
		return PageResponse.from(salePage.map(sale -> SaleResponse.from(
			sale,
			List.of(),
			usernames,
			comboLinesBySaleId.getOrDefault(sale.getId(), List.of())
		)), normalizedPage, normalizedPageSize);
	}

	@Transactional(readOnly = true)
	public SaleResponse getSale(AuthenticatedUser user, UUID saleId) {
		return toResponse(loadSale(user, saleId));
	}

	@Transactional
	public SaleResponse createSale(AuthenticatedUser user, UpsertSaleRequest request) {
		CompanyEntity company = loadCompany(user.companyId());
		BranchEntity branch = loadBranch(user, request.branchId() == null ? user.branchId() : request.branchId());
		CustomerEntity customer = loadCustomerOrGeneric(user.companyId(), request.customerId());
		SaleStatus status = normalizeWritableStatus(request.status());
		ensureHasSaleLines(request);
		ensureDiscountPermission(user, request);
		String saleNumber = normalizeSaleNumber(request.saleNumber());
		if (saleNumber == null) {
			saleNumber = generateSaleNumber(user.companyId());
		} else {
			validateSaleNumberAvailable(user.companyId(), saleNumber);
		}

		SaleEntity sale = new SaleEntity();
		sale.setCompany(company);
		sale.setBranch(branch);
		sale.setCustomer(customer);
		sale.setSaleNumber(saleNumber);
		sale.setSaleDate(Instant.now());
		sale.setCreatedBy(user.userId());
		sale.setTimezoneSnapshot(resolveCompanyTimezone(company));
		sale.setSubtotal(ZERO);
		sale.setTaxAmount(ZERO);
		sale.setTotalAmount(ZERO);
		applySaleFields(sale, request, status);
		SaleEntity savedSale = saleRepository.save(sale);

		saveDetails(user.companyId(), savedSale, request.details());
		saveComboLines(user.companyId(), savedSale, request.comboLines());
		recalculateTotals(savedSale);
		return toResponse(savedSale);
	}

	@Transactional
	public SaleResponse updateSale(AuthenticatedUser user, UUID saleId, UpsertSaleRequest request) {
		SaleEntity sale = loadSaleForUpdate(user, saleId);
		ensureWritable(sale);
		BranchEntity branch = loadBranch(user, request.branchId() == null ? user.branchId() : request.branchId());
		CustomerEntity customer = loadCustomerOrGeneric(user.companyId(), request.customerId());
		SaleStatus status = normalizeWritableStatus(request.status());
		ensureHasSaleLines(request);
		ensureDiscountPermission(user, request);
		String saleNumber = normalizeSaleNumber(request.saleNumber());
		if (saleNumber != null && !saleNumber.equalsIgnoreCase(sale.getSaleNumber())) {
			validateSaleNumberAvailable(user.companyId(), saleNumber);
			sale.setSaleNumber(saleNumber);
		}

		sale.setBranch(branch);
		sale.setCustomer(customer);
		applySaleFields(sale, request, status);
		saleDetailRepository.findBySale_IdOrderByCreatedAtAsc(sale.getId()).forEach(saleDetailRepository::delete);
		saleDetailRepository.flush();
		saleComboLineRepository.findBySale_IdOrderByCreatedAtAsc(sale.getId()).forEach(saleComboLineRepository::delete);
		saleComboLineRepository.flush();
		saveDetails(user.companyId(), sale, request.details());
		saveComboLines(user.companyId(), sale, request.comboLines());
		recalculateTotals(sale);
		return toResponse(sale);
	}

	@Transactional
	public SaleResponse confirmSale(AuthenticatedUser user, UUID saleId) {
		SaleEntity sale = loadSaleForUpdate(user, saleId);
		SaleStatus oldStatus = sale.getStatus();
		if (sale.getStatus() == SaleStatus.CONFIRMED) {
			throw ApiException.badRequest(ErrorCode.INVALID_SALE_STATUS, "Sale is already confirmed");
		}
		if (sale.getStatus() == SaleStatus.CANCELLED) {
			throw ApiException.badRequest(ErrorCode.INVALID_SALE_STATUS, "Cancelled sale cannot be confirmed");
		}

		List<SaleDetailEntity> details = saleDetailRepository.findBySale_IdOrderByCreatedAtAsc(sale.getId());
		if (details.isEmpty()) {
			throw ApiException.badRequest(ErrorCode.INVALID_SALE_STATUS, "Sale must have at least one detail");
		}

		for (SaleDetailEntity detail : details) {
			StockOperationResponse stockOperation = inventoryService.issueStock(
				user,
				sale.getBranch().getId(),
				null,
				detail.getProduct().getId(),
				detail.getQuantityBase(),
				StockMovementType.SALE_OUT,
				SALE_DETAIL_SOURCE,
				detail.getId(),
				"Sale " + sale.getSaleNumber()
			);
			detail.setFifoCostTotal(stockOperation.movement().totalCost());
		}

		sale.setConfirmedAt(Instant.now());
		sale.setStatus(SaleStatus.CONFIRMED);
		auditLogService.log(user, sale.getBranch().getId(), AuditAction.CONFIRM, "sale", sale.getId(), jsonStatus(oldStatus), jsonStatus(sale.getStatus()));
		return toResponse(sale);
	}

	@Transactional
	public SaleResponse cancelSale(AuthenticatedUser user, UUID saleId, CancelSaleRequest request) {
		SaleEntity sale = loadSaleForUpdate(user, saleId);
		SaleStatus oldStatus = sale.getStatus();
		if (sale.getStatus() == SaleStatus.CANCELLED) {
			throw ApiException.badRequest(ErrorCode.INVALID_SALE_STATUS, "Sale is already cancelled");
		}
		if (sale.getStatus() != SaleStatus.CONFIRMED) {
			throw ApiException.badRequest(ErrorCode.INVALID_SALE_STATUS, "Only confirmed sales can be cancelled");
		}

		String reason = trimToNull(request.reason());
		if (reason == null) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Cancel reason is required");
		}

		String disposition = trimToNull(request.cancelDisposition());
		if (disposition == null) {
			disposition = "RETURN_TO_STOCK";
		}
		if (!isValidDisposition(disposition)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Invalid cancel disposition: " + disposition);
		}

		List<SaleDetailEntity> details = saleDetailRepository.findBySale_IdOrderByCreatedAtAsc(sale.getId());
		for (SaleDetailEntity detail : details) {
			BigDecimal unitCost = detail.getFifoCostTotal().divide(detail.getQuantityBase(), 4, RoundingMode.HALF_UP);
			switch (disposition) {
				case "RETURN_TO_STOCK" -> inventoryService.receiveStock(
					user,
					sale.getBranch().getId(),
					null,
					detail.getProduct().getId(),
					detail.getQuantityBase(),
					unitCost,
					null,
					null,
					StockMovementType.SALE_CANCEL_IN,
					SALE_CANCEL_SOURCE,
					detail.getId(),
					null,
					reason
				);
				case "DAMAGED" -> {
					// First return to stock to reverse original deduction,
					// then issue as damaged so net inventory is -saleQty (not double).
					inventoryService.receiveStock(
						user,
						sale.getBranch().getId(),
						null,
						detail.getProduct().getId(),
						detail.getQuantityBase(),
						unitCost,
						null,
						null,
						StockMovementType.SALE_CANCEL_IN,
						SALE_CANCEL_SOURCE,
						detail.getId(),
						null,
						reason
					);
					inventoryService.issueStock(
						user,
						sale.getBranch().getId(),
						null,
						detail.getProduct().getId(),
						detail.getQuantityBase(),
						StockMovementType.DAMAGE_OUT,
						SALE_CANCEL_SOURCE,
						detail.getId(),
						"Sale cancel (damaged): " + reason
					);
				}
				case "LOSS" -> {
					// Same pattern: return to stock first, then issue as loss.
					inventoryService.receiveStock(
						user,
						sale.getBranch().getId(),
						null,
						detail.getProduct().getId(),
						detail.getQuantityBase(),
						unitCost,
						null,
						null,
						StockMovementType.SALE_CANCEL_IN,
						SALE_CANCEL_SOURCE,
						detail.getId(),
						null,
						reason
					);
					inventoryService.issueStock(
						user,
						sale.getBranch().getId(),
						null,
						detail.getProduct().getId(),
						detail.getQuantityBase(),
						StockMovementType.LOSS_OUT,
						SALE_CANCEL_SOURCE,
						detail.getId(),
						"Sale cancel (loss): " + reason
					);
				}
				case "DO_NOT_RETURN" -> {
					// No inventory movement; only cancel the sale record
				}
			}
		}

		sale.setStatus(SaleStatus.CANCELLED);
		sale.setCancelledBy(user.userId());
		sale.setCancelReason(reason);
		sale.setCancelDisposition(disposition);
		sale.setCancelledAt(Instant.now());
		auditLogService.log(user, sale.getBranch().getId(), AuditAction.CANCEL, "sale", sale.getId(), jsonStatus(oldStatus), jsonStatus(sale.getStatus()));
		return toResponse(sale);
	}

	private boolean isValidDisposition(String disposition) {
		return "RETURN_TO_STOCK".equals(disposition)
			|| "DAMAGED".equals(disposition)
			|| "LOSS".equals(disposition)
			|| "DO_NOT_RETURN".equals(disposition);
	}

	@Transactional
	public long deleteDraftSales(AuthenticatedUser user, UUID branchId) {
		UUID scopedBranchId = resolveScopedBranchId(user, branchId);
		long deletedCount = saleRepository.countDrafts(user.companyId(), scopedBranchId);
		if (deletedCount == 0) {
			return 0;
		}

		saleDetailRepository.deleteDraftDetails(user.companyId(), scopedBranchId);
		saleComboLineRepository.deleteDraftComboLines(user.companyId(), scopedBranchId);
		saleRepository.deleteDrafts(user.companyId(), scopedBranchId);
		return deletedCount;
	}

	@Transactional
	public void deleteDraftSale(AuthenticatedUser user, UUID saleId) {
		SaleEntity sale = loadSaleForUpdate(user, saleId);
		if (sale.getStatus() != SaleStatus.DRAFT) {
			throw ApiException.badRequest(ErrorCode.INVALID_SALE_STATUS, "Only draft sales can be deleted");
		}

		saleDetailRepository.findBySale_IdOrderByCreatedAtAsc(sale.getId()).forEach(saleDetailRepository::delete);
		saleDetailRepository.flush();
		saleComboLineRepository.findBySale_IdOrderByCreatedAtAsc(sale.getId()).forEach(saleComboLineRepository::delete);
		saleComboLineRepository.flush();
		saleRepository.delete(sale);
	}

	private void applySaleFields(SaleEntity sale, UpsertSaleRequest request, SaleStatus status) {
		sale.setPaymentMethod(request.paymentMethod());
		sale.setDiscountAmount(nonNull(request.discountAmount()));
		sale.setStatus(status);
		sale.setNotes(trimToNull(request.notes()));
	}

	private void ensureDiscountPermission(AuthenticatedUser user, UpsertSaleRequest request) {
		boolean hasSaleDiscount = nonNull(request.discountAmount()).signum() > 0;
		boolean hasLineDiscount = request.details() != null && request.details().stream()
			.anyMatch(detail -> nonNull(detail.discountAmount()).signum() > 0);
		if ((hasSaleDiscount || hasLineDiscount) && !user.permissions().contains("sales.apply_discount")) {
			throw ApiException.forbidden(ErrorCode.PERMISSION_DENIED, "Missing required permission: sales.apply_discount");
		}
	}

	private void ensureHasSaleLines(UpsertSaleRequest request) {
		boolean hasDetails = request.details() != null && !request.details().isEmpty();
		boolean hasComboLines = request.comboLines() != null && !request.comboLines().isEmpty();
		if (!hasDetails && !hasComboLines) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Sale must have at least one product or combo");
		}
	}

	private void saveDetails(UUID companyId, SaleEntity sale, List<SaleDetailRequest> detailRequests) {
		if (detailRequests == null || detailRequests.isEmpty()) {
			return;
		}
		for (SaleDetailRequest detailRequest : detailRequests) {
			ProductPresentationEntity presentation = loadPresentation(companyId, detailRequest);
			ProductEntity product = presentation.getProduct();
			if (!presentation.isActive() || !product.isActive() || !product.isStockable()) {
				throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Presentation product must be active and stockable");
			}

			BigDecimal conversionFactor = presentation.getConversionFactor();
			BigDecimal quantityPresentation = detailRequest.quantityPresentation();
			BigDecimal quantityBase = quantityPresentation.multiply(conversionFactor);
			BigDecimal unitPrice = detailRequest.unitPrice() == null ? presentation.getSalePrice() : detailRequest.unitPrice();
			BigDecimal lineSubtotal = quantityPresentation.multiply(unitPrice);
			BigDecimal discountAmount = nonNull(detailRequest.discountAmount());
			BigDecimal taxAmount = nonNull(detailRequest.taxAmount());
			BigDecimal totalAmount = lineSubtotal.add(taxAmount).subtract(discountAmount);
			if (totalAmount.signum() < 0) {
				throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Line discount cannot exceed line total");
			}

			SaleDetailEntity detail = new SaleDetailEntity();
			detail.setSale(sale);
			detail.setProduct(product);
			detail.setPresentation(presentation);
			detail.setBarcode(trimToNull(detailRequest.barcode()) == null ? presentation.getBarcode() : trimToNull(detailRequest.barcode()));
			detail.setQuantityPresentation(quantityPresentation);
			detail.setConversionFactor(conversionFactor);
			detail.setQuantityBase(quantityBase);
			detail.setUnitPrice(unitPrice);
			detail.setDiscountAmount(discountAmount);
			detail.setTaxAmount(taxAmount);
			detail.setTotalAmount(totalAmount);
			detail.setFifoCostTotal(ZERO);
			detail.setSourceType(SaleDetailSourceType.NORMAL);
			saleDetailRepository.save(detail);
		}
	}

	private void saveComboLines(UUID companyId, SaleEntity sale, List<SaleComboLineRequest> comboLineRequests) {
		if (comboLineRequests == null || comboLineRequests.isEmpty()) {
			return;
		}

		for (SaleComboLineRequest comboLineRequest : comboLineRequests) {
			ComboOfferEntity combo = comboOfferRepository.findByIdAndCompany_IdAndDeletedAtIsNull(comboLineRequest.comboOfferId(), companyId)
				.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Combo not found"));
			if (!combo.isActive()) {
				throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo must be active");
			}
			Instant now = Instant.now();
			if ((combo.getStartAt() != null && now.isBefore(combo.getStartAt()))
				|| (combo.getEndAt() != null && now.isAfter(combo.getEndAt()))) {
				throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo is outside its availability period");
			}

			BigDecimal comboQuantity = comboLineRequest.quantity();
			BigDecimal comboUnitPrice = combo.getComboPrice();
			BigDecimal comboTotal = comboUnitPrice.multiply(comboQuantity);

			// Register the combo line for traceability
			SaleComboLineEntity comboLine = new SaleComboLineEntity();
			comboLine.setSale(sale);
			comboLine.setComboOffer(combo);
			comboLine.setComboName(combo.getName());
			comboLine.setComboBarcode(combo.getBarcode());
			comboLine.setQuantity(comboQuantity);
			comboLine.setComboUnitPrice(comboUnitPrice);
			comboLine.setTotalAmount(comboTotal);
			SaleComboLineEntity savedComboLine = saleComboLineRepository.save(comboLine);

			// Expand combo items into individual sale details for stock/FIFO consumption
			for (ComboOfferItemEntity item : combo.getItems()) {
				if (!item.getProduct().isActive() || !item.getProduct().isStockable()) {
					throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo product must be active and stockable");
				}
				if (!item.getPresentation().isActive()) {
					throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Combo presentation must be active");
				}

				BigDecimal quantityPresentation = item.getQuantityPresentation().multiply(comboQuantity);
				BigDecimal conversionFactor = item.getConversionFactor();
				BigDecimal quantityBase = quantityPresentation.multiply(conversionFactor);
				BigDecimal unitPrice = BigDecimal.ZERO; // Combo price is tracked at combo line level, not per component
				BigDecimal lineSubtotal = BigDecimal.ZERO;
				BigDecimal discountAmount = BigDecimal.ZERO;
				BigDecimal taxAmount = BigDecimal.ZERO;
				BigDecimal totalAmount = BigDecimal.ZERO;

				SaleDetailEntity detail = new SaleDetailEntity();
				detail.setSale(sale);
				detail.setProduct(item.getProduct());
				detail.setPresentation(item.getPresentation());
				detail.setBarcode(item.getPresentation().getBarcode());
				detail.setQuantityPresentation(quantityPresentation);
				detail.setConversionFactor(conversionFactor);
				detail.setQuantityBase(quantityBase);
				detail.setUnitPrice(unitPrice);
				detail.setDiscountAmount(discountAmount);
				detail.setTaxAmount(taxAmount);
				detail.setTotalAmount(totalAmount);
				detail.setFifoCostTotal(ZERO);
				detail.setSourceType(SaleDetailSourceType.COMBO_LINE);
				detail.setComboLineId(savedComboLine.getId());
				saleDetailRepository.save(detail);
			}
		}
	}

	private ProductPresentationEntity loadPresentation(UUID companyId, SaleDetailRequest detailRequest) {
		if (detailRequest.presentationId() != null) {
			return productPresentationRepository.findByIdAndCompany_IdAndDeletedAtIsNull(detailRequest.presentationId(), companyId)
				.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Presentation not found"));
		}

		String barcode = trimToNull(detailRequest.barcode());
		if (barcode == null) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Presentation or barcode is required");
		}

		return productPresentationRepository.findByCompany_IdAndBarcodeAndDeletedAtIsNull(companyId, barcode)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.BARCODE_NOT_FOUND, "Barcode not found"));
	}

	private void recalculateTotals(SaleEntity sale) {
		List<SaleDetailEntity> details = saleDetailRepository.findBySale_IdOrderByCreatedAtAsc(sale.getId());
		BigDecimal detailTotal = details.stream().map(SaleDetailEntity::getTotalAmount).reduce(ZERO, BigDecimal::add);
		BigDecimal lineDiscounts = details.stream().map(SaleDetailEntity::getDiscountAmount).reduce(ZERO, BigDecimal::add);
		BigDecimal taxAmount = details.stream().map(SaleDetailEntity::getTaxAmount).reduce(ZERO, BigDecimal::add);
		List<SaleComboLineEntity> comboLines = saleComboLineRepository.findBySale_IdOrderByCreatedAtAsc(sale.getId());
		BigDecimal comboTotal = comboLines.stream().map(SaleComboLineEntity::getTotalAmount).reduce(ZERO, BigDecimal::add);
		BigDecimal saleDiscount = nonNull(sale.getDiscountAmount());
		BigDecimal totalAmount = detailTotal.add(comboTotal).subtract(saleDiscount);
		if (totalAmount.signum() < 0) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Sale discount cannot exceed sale total");
		}

		sale.setSubtotal(detailTotal.add(comboTotal).subtract(taxAmount).add(lineDiscounts));
		sale.setTaxAmount(taxAmount);
		sale.setDiscountAmount(saleDiscount.add(lineDiscounts));
		sale.setTotalAmount(totalAmount);
	}

	private SaleResponse toResponse(SaleEntity sale) {
		List<SaleDetailResponse> details = saleDetailRepository.findBySale_IdOrderByCreatedAtAsc(sale.getId())
			.stream()
			.map(SaleDetailResponse::from)
			.toList();
		List<SaleComboLineEntity> comboLines = saleComboLineRepository.findBySale_IdOrderByCreatedAtAsc(sale.getId());
		return SaleResponse.from(sale, details, usernameMap(sale.getCompany().getId(), List.of(sale)), comboLines);
	}

	private Map<UUID, String> usernameMap(UUID companyId, List<SaleEntity> sales) {
		Set<UUID> userIds = new HashSet<>();
		for (SaleEntity sale : sales) {
			addIfNotNull(userIds, sale.getCreatedBy());
			addIfNotNull(userIds, sale.getCancelledBy());
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

	private String jsonStatus(SaleStatus status) {
		return "{\"status\":\"" + status.name() + "\"}";
	}

	private SaleEntity loadSale(UUID companyId, UUID saleId) {
		return saleRepository.findByIdAndCompany_Id(saleId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Sale not found"));
	}

	private SaleEntity loadSale(AuthenticatedUser user, UUID saleId) {
		SaleEntity sale = loadSale(user.companyId(), saleId);
		ensureCanAccessBranch(user, sale.getBranch().getId());
		return sale;
	}

	private SaleEntity loadSaleForUpdate(AuthenticatedUser user, UUID saleId) {
		SaleEntity sale = saleRepository.findByIdAndCompanyIdForUpdate(saleId, user.companyId())
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Sale not found"));
		ensureCanAccessBranch(user, sale.getBranch().getId());
		return sale;
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

	private UUID resolveScopedBranchId(AuthenticatedUser user, UUID branchId) {
		if (canAccessAllBranches(user)) {
			return branchId;
		}
		if (branchId != null && !user.branchId().equals(branchId)) {
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

	private CustomerEntity loadCustomerOrGeneric(UUID companyId, UUID customerId) {
		CustomerEntity customer = customerId == null
			? customerRepository.findFirstByCompany_IdAndGenericConsumerTrueAndDeletedAtIsNull(companyId)
				.orElse(null)
			: customerRepository.findByIdAndCompany_IdAndDeletedAtIsNull(customerId, companyId)
				.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Customer not found"));
		if (customer != null && !customer.isActive()) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Customer must be active");
		}
		return customer;
	}

	private SaleStatus normalizeWritableStatus(SaleStatus status) {
		SaleStatus normalizedStatus = status == null ? SaleStatus.DRAFT : status;
		if (normalizedStatus != SaleStatus.DRAFT) {
			throw ApiException.badRequest(ErrorCode.INVALID_SALE_STATUS, "Sale can only be saved as draft");
		}
		return normalizedStatus;
	}

	private void ensureWritable(SaleEntity sale) {
		if (sale.getStatus() == SaleStatus.CONFIRMED || sale.getStatus() == SaleStatus.CANCELLED) {
			throw ApiException.badRequest(ErrorCode.INVALID_SALE_STATUS, "Confirmed or cancelled sale cannot be edited");
		}
	}

	private void validateSaleNumberAvailable(UUID companyId, String saleNumber) {
		if (saleRepository.existsByCompany_IdAndSaleNumberIgnoreCase(companyId, saleNumber)) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Sale number already exists in the company");
		}
	}

	private String generateSaleNumber(UUID companyId) {
		String saleNumber;
		do {
			saleNumber = "SAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
		} while (saleRepository.existsByCompany_IdAndSaleNumberIgnoreCase(companyId, saleNumber));
		return saleNumber;
	}

	private BigDecimal nonNull(BigDecimal value) {
		return value == null ? ZERO : value;
	}

	private String normalizeQuery(String query) {
		String trimmed = trimToNull(query);
		return trimmed == null ? "" : trimmed;
	}

	private Specification<SaleEntity> saleSearch(UUID companyId, UUID branchId, UUID customerId, SaleStatus status, SalePaymentMethod paymentMethod, Instant fromDate, Instant toDate, String query) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			Predicate predicate = criteriaBuilder.equal(root.get("company").get("id"), companyId);
			if (branchId != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("branch").get("id"), branchId));
			}
			if (customerId != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("customer").get("id"), customerId));
			}
			if (status != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
			}
			if (paymentMethod != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("paymentMethod"), paymentMethod));
			}
			if (fromDate != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("saleDate"), fromDate));
			}
			if (toDate != null) {
				predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThanOrEqualTo(root.get("saleDate"), toDate));
			}
			if (!query.isBlank()) {
				String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
				Join<SaleEntity, CustomerEntity> customer = root.join("customer", JoinType.LEFT);
				Predicate queryPredicate = criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("saleNumber")), pattern),
					criteriaBuilder.like(criteriaBuilder.lower(customer.get("name")), pattern)
				);
				predicate = criteriaBuilder.and(predicate, queryPredicate);
			}
			return predicate;
		};
	}

	private String normalizeSaleNumber(String saleNumber) {
		String trimmed = trimToNull(saleNumber);
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
