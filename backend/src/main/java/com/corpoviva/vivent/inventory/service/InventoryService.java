package com.corpoviva.vivent.inventory.service;

import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.branches.entity.LocationEntity;
import com.corpoviva.vivent.branches.repository.BranchRepository;
import com.corpoviva.vivent.branches.repository.LocationRepository;
import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.inventory.dto.InventoryBalanceResponse;
import com.corpoviva.vivent.inventory.dto.InventoryLayerResponse;
import com.corpoviva.vivent.inventory.dto.InventorySummaryResponse;
import com.corpoviva.vivent.inventory.dto.StockInRequest;
import com.corpoviva.vivent.inventory.dto.StockMovementResponse;
import com.corpoviva.vivent.inventory.dto.StockOperationResponse;
import com.corpoviva.vivent.inventory.dto.StockOutRequest;
import com.corpoviva.vivent.inventory.entity.InventoryBalanceEntity;
import com.corpoviva.vivent.inventory.entity.InventoryLayerEntity;
import com.corpoviva.vivent.inventory.entity.InventoryLayerStatus;
import com.corpoviva.vivent.inventory.entity.StockMovementEntity;
import com.corpoviva.vivent.inventory.entity.StockMovementType;
import com.corpoviva.vivent.inventory.repository.InventoryBalanceRepository;
import com.corpoviva.vivent.inventory.repository.InventoryLayerRepository;
import com.corpoviva.vivent.inventory.repository.StockMovementRepository;
import com.corpoviva.vivent.products.entity.ProductEntity;
import com.corpoviva.vivent.products.service.ProductService;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.users.entity.UserEntity;
import com.corpoviva.vivent.users.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
public class InventoryService {

	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final String COMPANY_ADMIN = "COMPANY_ADMIN";
	private static final String MANUAL_STOCK_IN = "MANUAL_STOCK_IN";
	private static final String MANUAL_STOCK_OUT = "MANUAL_STOCK_OUT";

	private final InventoryBalanceRepository inventoryBalanceRepository;
	private final InventoryLayerRepository inventoryLayerRepository;
	private final StockMovementRepository stockMovementRepository;
	private final BranchRepository branchRepository;
	private final LocationRepository locationRepository;
	private final ProductService productService;
	private final UserRepository userRepository;

	public InventoryService(
		InventoryBalanceRepository inventoryBalanceRepository,
		InventoryLayerRepository inventoryLayerRepository,
		StockMovementRepository stockMovementRepository,
		BranchRepository branchRepository,
		LocationRepository locationRepository,
		ProductService productService,
		UserRepository userRepository
	) {
		this.inventoryBalanceRepository = inventoryBalanceRepository;
		this.inventoryLayerRepository = inventoryLayerRepository;
		this.stockMovementRepository = stockMovementRepository;
		this.branchRepository = branchRepository;
		this.locationRepository = locationRepository;
		this.productService = productService;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<InventoryBalanceResponse> listBalances(
		AuthenticatedUser user,
		int page,
		int pageSize,
		UUID branchId,
		UUID locationId,
		UUID productId,
		UUID categoryId,
		String query
	) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		UUID scopedBranchId = scopedBranchId(user, branchId);
		Page<InventoryBalanceEntity> balancePage = inventoryBalanceRepository.findAll(
			buildBalanceSpecification(
				user.companyId(),
				scopedBranchId,
				locationId,
				productId,
				categoryId,
				normalizeQuery(query)
			),
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("updatedAt").descending())
		);

		return PageResponse.from(balancePage.map(InventoryBalanceResponse::from), normalizedPage, normalizedPageSize);
	}

	private Specification<InventoryBalanceEntity> buildBalanceSpecification(
		UUID companyId,
		UUID branchId,
		UUID locationId,
		UUID productId,
		UUID categoryId,
		String query
	) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("company").get("id"), companyId));

			if (branchId != null) {
				predicates.add(criteriaBuilder.equal(root.get("branch").get("id"), branchId));
			}

			if (locationId != null) {
				predicates.add(criteriaBuilder.equal(root.get("location").get("id"), locationId));
			}

			Join<Object, Object> product = null;
			if (productId != null || categoryId != null || query != null) {
				product = root.join("product");
			}

			if (productId != null && product != null) {
				predicates.add(criteriaBuilder.equal(product.get("id"), productId));
			}

			if (categoryId != null && product != null) {
				predicates.add(criteriaBuilder.equal(product.join("category", JoinType.LEFT).get("id"), categoryId));
			}

			if (query != null && product != null) {
				String pattern = "%" + query.toLowerCase() + "%";
				predicates.add(criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(product.get("name")), pattern),
					criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(product.get("sku"), "")), pattern)
				));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	@Transactional(readOnly = true)
	public PageResponse<InventoryLayerResponse> listLayers(
		AuthenticatedUser user,
		int page,
		int pageSize,
		UUID branchId,
		UUID locationId,
		UUID productId,
		InventoryLayerStatus status
	) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		UUID scopedBranchId = scopedBranchId(user, branchId);
		Page<InventoryLayerEntity> layerPage = inventoryLayerRepository.findAll(
			buildLayerSpecification(user.companyId(), scopedBranchId, locationId, productId, status),
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("receivedAt").ascending())
		);

		return PageResponse.from(layerPage.map(InventoryLayerResponse::from), normalizedPage, normalizedPageSize);
	}

	private Specification<InventoryLayerEntity> buildLayerSpecification(
		UUID companyId,
		UUID branchId,
		UUID locationId,
		UUID productId,
		InventoryLayerStatus status
	) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("company").get("id"), companyId));

			if (branchId != null) {
				predicates.add(criteriaBuilder.equal(root.get("branch").get("id"), branchId));
			}

			if (locationId != null) {
				predicates.add(criteriaBuilder.equal(root.get("location").get("id"), locationId));
			}

			if (productId != null) {
				predicates.add(criteriaBuilder.equal(root.get("product").get("id"), productId));
			}

			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	@Transactional(readOnly = true)
	public PageResponse<StockMovementResponse> getKardex(
		AuthenticatedUser user,
		int page,
		int pageSize,
		UUID branchId,
		UUID locationId,
		UUID productId,
		StockMovementType movementType,
		Instant fromDate,
		Instant toDate
	) {
		return listMovements(user, page, pageSize, branchId, locationId, productId, movementType, fromDate, toDate);
	}

	@Transactional(readOnly = true)
	public PageResponse<StockMovementResponse> listMovements(
		AuthenticatedUser user,
		int page,
		int pageSize,
		UUID branchId,
		UUID locationId,
		UUID productId,
		StockMovementType movementType,
		Instant fromDate,
		Instant toDate
	) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		UUID scopedBranchId = scopedBranchId(user, branchId);
		Page<StockMovementEntity> movementPage = stockMovementRepository.findAll(
			buildMovementSpecification(user.companyId(), scopedBranchId, locationId, productId, movementType, fromDate, toDate),
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("createdAt").descending())
		);

		Map<UUID, String> usernames = usernameMap(user.companyId(), movementPage.getContent());
		return PageResponse.from(movementPage.map(movement -> StockMovementResponse.from(movement, usernames)), normalizedPage, normalizedPageSize);
	}

	private Specification<StockMovementEntity> buildMovementSpecification(
		UUID companyId,
		UUID branchId,
		UUID locationId,
		UUID productId,
		StockMovementType movementType,
		Instant fromDate,
		Instant toDate
	) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("company").get("id"), companyId));

			if (branchId != null) {
				predicates.add(criteriaBuilder.equal(root.get("branch").get("id"), branchId));
			}

			if (locationId != null) {
				predicates.add(criteriaBuilder.equal(root.get("location").get("id"), locationId));
			}

			if (productId != null) {
				predicates.add(criteriaBuilder.equal(root.get("product").get("id"), productId));
			}

			if (movementType != null) {
				predicates.add(criteriaBuilder.equal(root.get("movementType"), movementType));
			}

			if (fromDate != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
			}

			if (toDate != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	@Transactional(readOnly = true)
	public InventorySummaryResponse getSummary(AuthenticatedUser user) {
		UUID scopedBranchId = canAccessAllBranches(user) ? null : user.branchId();
		return new InventorySummaryResponse(
			inventoryBalanceRepository.countProductsWithStock(user.companyId(), scopedBranchId),
			inventoryBalanceRepository.countProductsWithoutStock(user.companyId(), scopedBranchId),
			inventoryBalanceRepository.countProductsWithLowStock(user.companyId(), scopedBranchId),
			stockMovementRepository.countByCompanyIdAndOptionalBranchId(user.companyId(), scopedBranchId)
		);
	}

	@Transactional
	public StockOperationResponse stockIn(AuthenticatedUser user, StockInRequest request) {
		ensureCanAccessBranch(user, request.branchId());
		return receiveStock(
			user,
			request.branchId(),
			request.locationId(),
			request.productId(),
			request.quantity(),
			request.unitCost(),
			request.expirationDate(),
			request.lotNumber(),
			StockMovementType.ADJUSTMENT_IN,
			MANUAL_STOCK_IN,
			UUID.randomUUID(),
			null,
			request.notes()
		);
	}

	@Transactional
	public StockOperationResponse receiveStock(
		AuthenticatedUser user,
		UUID branchId,
		UUID locationId,
		UUID productId,
		BigDecimal quantity,
		BigDecimal unitCost,
		LocalDate expirationDate,
		String lotNumber,
		StockMovementType movementType,
		String sourceType,
		UUID sourceId,
		UUID purchaseDetailId,
		String notes
	) {
		BranchEntity branch = loadBranch(user.companyId(), branchId);
		LocationEntity location = loadLocation(user.companyId(), branch.getId(), locationId);
		ProductEntity product = loadStockableProduct(user.companyId(), productId);
		UUID operationId = sourceId == null ? UUID.randomUUID() : sourceId;

		InventoryBalanceEntity balance = loadOrCreateBalance(user.companyId(), branch, location, product);
		balance.setQuantityOnHand(balance.getQuantityOnHand().add(quantity));

		InventoryLayerEntity layer = new InventoryLayerEntity();
		layer.setCompany(product.getCompany());
		layer.setBranch(branch);
		layer.setLocation(location);
		layer.setProduct(product);
		layer.setPurchaseDetailId(purchaseDetailId);
		layer.setSourceType(sourceType);
		layer.setSourceId(operationId);
		layer.setQuantityReceived(quantity);
		layer.setQuantityRemaining(quantity);
		layer.setUnitCost(unitCost);
		layer.setReceivedAt(Instant.now());
		layer.setExpirationDate(expirationDate);
		layer.setLotNumber(trimToNull(lotNumber));
		layer.setStatus(InventoryLayerStatus.OPEN);
		layer.setTimezoneSnapshot(resolveCompanyTimezone(product));
		InventoryLayerEntity savedLayer = inventoryLayerRepository.save(layer);

		StockMovementEntity movement = createMovement(
			user,
			branch,
			location,
			product,
			movementType,
			sourceType,
			operationId,
			quantity,
			ZERO,
			unitCost,
			quantity.multiply(unitCost),
			balance.getQuantityOnHand(),
			notes
		);

		return new StockOperationResponse(
			operationId,
			InventoryBalanceResponse.from(balance),
			StockMovementResponse.from(movement, usernameMap(user.companyId(), List.of(movement))),
			List.of(InventoryLayerResponse.from(savedLayer))
		);
	}

	@Transactional
	public StockOperationResponse stockOut(AuthenticatedUser user, StockOutRequest request) {
		ensureCanAccessBranch(user, request.branchId());
		return issueStock(
			user,
			request.branchId(),
			request.locationId(),
			request.productId(),
			request.quantity(),
			StockMovementType.ADJUSTMENT_OUT,
			MANUAL_STOCK_OUT,
			UUID.randomUUID(),
			request.notes()
		);
	}

	@Transactional
	public StockOperationResponse reserveStock(
		AuthenticatedUser user,
		UUID branchId,
		UUID locationId,
		UUID productId,
		BigDecimal quantity,
		String sourceType,
		UUID sourceId,
		String notes
	) {
		BranchEntity branch = loadBranch(user.companyId(), branchId);
		LocationEntity location = loadLocation(user.companyId(), branch.getId(), locationId);
		ProductEntity product = loadStockableProduct(user.companyId(), productId);
		InventoryBalanceEntity balance = inventoryBalanceRepository
			.findByCompany_IdAndBranch_IdAndLocation_IdAndProduct_Id(user.companyId(), branch.getId(), location.getId(), product.getId())
			.orElseThrow(() -> ApiException.badRequest(ErrorCode.INSUFFICIENT_STOCK, "Insufficient stock"));

		BigDecimal available = balance.getQuantityOnHand().subtract(balance.getQuantityReserved());
		if (available.compareTo(quantity) < 0) {
			throw ApiException.badRequest(ErrorCode.INSUFFICIENT_STOCK, "Insufficient stock to reserve");
		}

		balance.setQuantityReserved(balance.getQuantityReserved().add(quantity));
		StockMovementEntity movement = createMovement(
			user,
			branch,
			location,
			product,
			StockMovementType.TRANSFER_RESERVED,
			sourceType,
			sourceId,
			ZERO,
			quantity,
			ZERO,
			ZERO,
			balance.getQuantityOnHand(),
			notes
		);

		return new StockOperationResponse(
			sourceId,
			InventoryBalanceResponse.from(balance),
			StockMovementResponse.from(movement, usernameMap(user.companyId(), List.of(movement))),
			List.of()
		);
	}

	@Transactional
	public void releaseReservedStock(AuthenticatedUser user, UUID branchId, UUID locationId, UUID productId, BigDecimal quantity) {
		BranchEntity branch = loadBranch(user.companyId(), branchId);
		LocationEntity location = loadLocation(user.companyId(), branch.getId(), locationId);
		ProductEntity product = loadStockableProduct(user.companyId(), productId);
		InventoryBalanceEntity balance = inventoryBalanceRepository
			.findByCompany_IdAndBranch_IdAndLocation_IdAndProduct_Id(user.companyId(), branch.getId(), location.getId(), product.getId())
			.orElseThrow(() -> ApiException.badRequest(ErrorCode.INSUFFICIENT_STOCK, "Reserved stock not found"));

		if (balance.getQuantityReserved().compareTo(quantity) < 0) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Reserved stock is lower than release quantity");
		}

		balance.setQuantityReserved(balance.getQuantityReserved().subtract(quantity));
	}

	@Transactional
	public StockOperationResponse voidPurchaseReceipt(
		AuthenticatedUser user,
		UUID branchId,
		UUID locationId,
		UUID productId,
		UUID purchaseDetailId,
		BigDecimal expectedQuantity,
		String sourceType,
		UUID sourceId,
		String notes
	) {
		ensureCanAccessBranch(user, branchId);
		if (expectedQuantity == null || expectedQuantity.signum() <= 0) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Void quantity must be greater than zero");
		}

		BranchEntity branch = loadBranch(user.companyId(), branchId);
		LocationEntity location = loadLocation(user.companyId(), branch.getId(), locationId);
		ProductEntity product = loadStockableProduct(user.companyId(), productId);
		InventoryBalanceEntity balance = inventoryBalanceRepository
			.findByCompany_IdAndBranch_IdAndLocation_IdAndProduct_Id(user.companyId(), branch.getId(), location.getId(), product.getId())
			.orElseThrow(() -> ApiException.badRequest(ErrorCode.INSUFFICIENT_STOCK, "Purchase stock not found"));

		List<InventoryLayerEntity> layers = inventoryLayerRepository.findPurchaseDetailLayersForUpdate(
			user.companyId(),
			branch.getId(),
			location.getId(),
			product.getId(),
			purchaseDetailId
		);
		if (layers.isEmpty()) {
			throw ApiException.badRequest(ErrorCode.INVALID_PURCHASE_STATUS, "Purchase inventory layer not found");
		}

		BigDecimal quantityToVoid = ZERO;
		BigDecimal totalCost = ZERO;
		for (InventoryLayerEntity layer : layers) {
			if (layer.getStatus() != InventoryLayerStatus.OPEN || layer.getQuantityRemaining().compareTo(layer.getQuantityReceived()) != 0) {
				throw ApiException.badRequest(ErrorCode.INVALID_PURCHASE_STATUS, "Purchase stock has already been consumed and cannot be voided exactly");
			}
			quantityToVoid = quantityToVoid.add(layer.getQuantityReceived());
			totalCost = totalCost.add(layer.getQuantityReceived().multiply(layer.getUnitCost()));
		}

		if (quantityToVoid.compareTo(expectedQuantity) != 0) {
			throw ApiException.badRequest(ErrorCode.INVALID_PURCHASE_STATUS, "Purchase inventory layers do not match purchase detail quantity");
		}

		BigDecimal available = balance.getQuantityOnHand().subtract(balance.getQuantityReserved());
		if (available.compareTo(quantityToVoid) < 0) {
			throw ApiException.badRequest(ErrorCode.INSUFFICIENT_STOCK, "Purchase stock is reserved and cannot be voided");
		}

		balance.setQuantityOnHand(balance.getQuantityOnHand().subtract(quantityToVoid));
		for (InventoryLayerEntity layer : layers) {
			layer.setQuantityRemaining(ZERO);
			layer.setStatus(InventoryLayerStatus.VOIDED);
		}

		UUID operationId = sourceId == null ? purchaseDetailId : sourceId;
		BigDecimal averageUnitCost = totalCost.divide(quantityToVoid, 4, RoundingMode.HALF_UP);
		StockMovementEntity movement = createMovement(
			user,
			branch,
			location,
			product,
			StockMovementType.PURCHASE_VOID_OUT,
			sourceType,
			operationId,
			ZERO,
			quantityToVoid,
			averageUnitCost,
			totalCost,
			balance.getQuantityOnHand(),
			notes
		);

		return new StockOperationResponse(
			operationId,
			InventoryBalanceResponse.from(balance),
			StockMovementResponse.from(movement, usernameMap(user.companyId(), List.of(movement))),
			layers.stream().map(InventoryLayerResponse::from).toList()
		);
	}

	@Transactional
	public StockOperationResponse issueStock(
		AuthenticatedUser user,
		UUID branchId,
		UUID locationId,
		UUID productId,
		BigDecimal quantity,
		StockMovementType movementType,
		String sourceType,
		UUID sourceId,
		String notes
	) {
		BranchEntity branch = loadBranch(user.companyId(), branchId);
		LocationEntity location = loadLocation(user.companyId(), branch.getId(), locationId);
		ProductEntity product = loadStockableProduct(user.companyId(), productId);
		UUID operationId = sourceId == null ? UUID.randomUUID() : sourceId;

		InventoryBalanceEntity balance = inventoryBalanceRepository
			.findByCompany_IdAndBranch_IdAndLocation_IdAndProduct_Id(user.companyId(), branch.getId(), location.getId(), product.getId())
			.orElseThrow(() -> ApiException.badRequest(ErrorCode.INSUFFICIENT_STOCK, "Insufficient stock"));

		BigDecimal available = balance.getQuantityOnHand().subtract(balance.getQuantityReserved());
		if (available.compareTo(quantity) < 0) {
			throw ApiException.badRequest(ErrorCode.INSUFFICIENT_STOCK, "Insufficient stock");
		}

		BigDecimal remainingToConsume = quantity;
		BigDecimal totalCost = ZERO;
		List<InventoryLayerEntity> affectedLayers = new ArrayList<>();
		List<InventoryLayerEntity> openLayers = inventoryLayerRepository.findOpenLayersForUpdate(
			user.companyId(),
			branch.getId(),
			location.getId(),
			product.getId()
		);

		for (InventoryLayerEntity layer : openLayers) {
			if (remainingToConsume.signum() == 0) {
				break;
			}

			BigDecimal consumed = layer.getQuantityRemaining().min(remainingToConsume);
			layer.setQuantityRemaining(layer.getQuantityRemaining().subtract(consumed));
			if (layer.getQuantityRemaining().signum() == 0) {
				layer.setStatus(InventoryLayerStatus.CONSUMED);
			}

			totalCost = totalCost.add(consumed.multiply(layer.getUnitCost()));
			remainingToConsume = remainingToConsume.subtract(consumed);
			affectedLayers.add(layer);
		}

		if (remainingToConsume.signum() > 0) {
			throw ApiException.badRequest(ErrorCode.INSUFFICIENT_STOCK, "Insufficient FIFO layers for stock out");
		}

		balance.setQuantityOnHand(balance.getQuantityOnHand().subtract(quantity));
		BigDecimal averageUnitCost = totalCost.divide(quantity, 4, RoundingMode.HALF_UP);

		StockMovementEntity movement = createMovement(
			user,
			branch,
			location,
			product,
			movementType,
			sourceType,
			operationId,
			ZERO,
			quantity,
			averageUnitCost,
			totalCost,
			balance.getQuantityOnHand(),
			notes
		);

		return new StockOperationResponse(
			operationId,
			InventoryBalanceResponse.from(balance),
			StockMovementResponse.from(movement, usernameMap(user.companyId(), List.of(movement))),
			affectedLayers.stream().map(InventoryLayerResponse::from).toList()
		);
	}

	private Map<UUID, String> usernameMap(UUID companyId, List<StockMovementEntity> movements) {
		Set<UUID> userIds = new HashSet<>();
		for (StockMovementEntity movement : movements) {
			addIfNotNull(userIds, movement.getCreatedBy());
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

	private StockMovementEntity createMovement(
		AuthenticatedUser user,
		BranchEntity branch,
		LocationEntity location,
		ProductEntity product,
		StockMovementType movementType,
		String sourceType,
		UUID sourceId,
		BigDecimal quantityIn,
		BigDecimal quantityOut,
		BigDecimal unitCost,
		BigDecimal totalCost,
		BigDecimal balanceAfter,
		String notes
	) {
		StockMovementEntity movement = new StockMovementEntity();
		movement.setCompany(product.getCompany());
		movement.setBranch(branch);
		movement.setLocation(location);
		movement.setProduct(product);
		movement.setMovementType(movementType);
		movement.setSourceType(sourceType);
		movement.setSourceId(sourceId);
		movement.setQuantityIn(quantityIn);
		movement.setQuantityOut(quantityOut);
		movement.setUnitCost(unitCost);
		movement.setTotalCost(totalCost);
		movement.setBalanceAfter(balanceAfter);
		movement.setNotes(trimToNull(notes));
		movement.setCreatedBy(user.userId());
		movement.setTimezoneSnapshot(resolveCompanyTimezone(product));
		return stockMovementRepository.save(movement);
	}

	private String resolveCompanyTimezone(ProductEntity product) {
		String timezone = product.getCompany().getTimezone();
		return timezone == null || timezone.isBlank() ? "UTC" : timezone;
	}

	private InventoryBalanceEntity loadOrCreateBalance(UUID companyId, BranchEntity branch, LocationEntity location, ProductEntity product) {
		return inventoryBalanceRepository.findByCompany_IdAndBranch_IdAndLocation_IdAndProduct_Id(
			companyId,
			branch.getId(),
			location.getId(),
			product.getId()
		).orElseGet(() -> {
			InventoryBalanceEntity balance = new InventoryBalanceEntity();
			balance.setCompany(product.getCompany());
			balance.setBranch(branch);
			balance.setLocation(location);
			balance.setProduct(product);
			balance.setQuantityOnHand(ZERO);
			balance.setQuantityReserved(ZERO);
			return inventoryBalanceRepository.save(balance);
		});
	}

	private ProductEntity loadStockableProduct(UUID companyId, UUID productId) {
		ProductEntity product = productService.loadProduct(companyId, productId);
		if (!product.isActive() || !product.isStockable()) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Product must be active and stockable");
		}

		return product;
	}

	private BranchEntity loadBranch(UUID companyId, UUID branchId) {
		BranchEntity branch = branchRepository.findByIdAndCompany_IdAndDeletedAtIsNull(branchId, companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Branch not found"));

		if (!branch.isActive()) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Branch must be active");
		}

		return branch;
	}

	private LocationEntity loadLocation(UUID companyId, UUID branchId, UUID locationId) {
		if (locationId == null) {
			return locationRepository.findByCompany_IdAndBranch_IdAndDefaultLocationTrueAndActiveTrue(companyId, branchId)
				.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Default location not found"));
		}

		LocationEntity location = locationRepository.findByIdAndCompany_IdAndBranch_Id(locationId, companyId, branchId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Location not found"));

		if (!location.isActive()) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Location must be active");
		}

		return location;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String normalizeQuery(String query) {
		return trimToNull(query);
	}
}
