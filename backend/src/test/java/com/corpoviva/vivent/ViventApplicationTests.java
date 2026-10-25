package com.corpoviva.vivent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.branches.repository.BranchRepository;
import com.corpoviva.vivent.combos.dto.ComboOfferItemRequest;
import com.corpoviva.vivent.combos.dto.UpsertComboOfferRequest;
import com.corpoviva.vivent.combos.repository.SaleComboLineRepository;
import com.corpoviva.vivent.combos.service.ComboOfferService;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.inventory.entity.InventoryLayerStatus;
import com.corpoviva.vivent.inventory.entity.StockMovementType;
import com.corpoviva.vivent.inventory.repository.InventoryBalanceRepository;
import com.corpoviva.vivent.inventory.repository.InventoryLayerRepository;
import com.corpoviva.vivent.inventory.repository.StockMovementRepository;
import com.corpoviva.vivent.inventory.service.InventoryService;
import com.corpoviva.vivent.platform.companies.service.PlatformCompanyService;
import com.corpoviva.vivent.platform.companies.dto.CreatePlatformCompanyRequest;
import com.corpoviva.vivent.products.repository.ProductPresentationRepository;
import com.corpoviva.vivent.purchases.dto.PurchaseDetailRequest;
import com.corpoviva.vivent.purchases.dto.UpsertPurchaseRequest;
import com.corpoviva.vivent.purchases.dto.VoidPurchaseRequest;
import com.corpoviva.vivent.purchases.entity.PaymentMethod;
import com.corpoviva.vivent.purchases.entity.PaymentType;
import com.corpoviva.vivent.purchases.entity.PurchaseStatus;
import com.corpoviva.vivent.purchases.repository.PurchaseDetailRepository;
import com.corpoviva.vivent.purchases.repository.SupplierRepository;
import com.corpoviva.vivent.purchases.service.PurchaseService;
import com.corpoviva.vivent.sales.dto.SaleComboLineRequest;
import com.corpoviva.vivent.sales.dto.UpsertSaleRequest;
import com.corpoviva.vivent.sales.entity.SaleDetailSourceType;
import com.corpoviva.vivent.sales.entity.SalePaymentMethod;
import com.corpoviva.vivent.sales.entity.SaleStatus;
import com.corpoviva.vivent.sales.repository.SaleDetailRepository;
import com.corpoviva.vivent.sales.repository.SaleRepository;
import com.corpoviva.vivent.sales.service.SaleService;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.users.dto.ChangeUserBranchRequest;
import com.corpoviva.vivent.users.dto.ChangeOwnPasswordRequest;
import com.corpoviva.vivent.users.repository.UserRepository;
import com.corpoviva.vivent.users.entity.UserStatus;
import com.corpoviva.vivent.users.service.UserProfileService;
import com.corpoviva.vivent.users.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class ViventApplicationTests {

	private final CompanyRepository companyRepository;
	private final PlatformCompanyService platformCompanyService;
	private final InventoryService inventoryService;
	private final BranchRepository branchRepository;
	private final SupplierRepository supplierRepository;
	private final ProductPresentationRepository productPresentationRepository;
	private final PurchaseService purchaseService;
	private final PurchaseDetailRepository purchaseDetailRepository;
	private final InventoryLayerRepository inventoryLayerRepository;
	private final InventoryBalanceRepository inventoryBalanceRepository;
	private final StockMovementRepository stockMovementRepository;
	private final UserRepository userRepository;
	private final UserService userService;
	private final UserProfileService userProfileService;
	private final PasswordEncoder passwordEncoder;
	private final ObjectMapper objectMapper;
	private final ComboOfferService comboOfferService;
	private final SaleService saleService;
	private final SaleRepository saleRepository;
	private final SaleDetailRepository saleDetailRepository;
	private final SaleComboLineRepository saleComboLineRepository;

	@Autowired
	ViventApplicationTests(
		CompanyRepository companyRepository,
		PlatformCompanyService platformCompanyService,
		InventoryService inventoryService,
		BranchRepository branchRepository,
		SupplierRepository supplierRepository,
		ProductPresentationRepository productPresentationRepository,
		PurchaseService purchaseService,
		PurchaseDetailRepository purchaseDetailRepository,
		InventoryLayerRepository inventoryLayerRepository,
		InventoryBalanceRepository inventoryBalanceRepository,
		StockMovementRepository stockMovementRepository,
		UserRepository userRepository,
		UserService userService,
		UserProfileService userProfileService,
		PasswordEncoder passwordEncoder,
		ObjectMapper objectMapper,
		ComboOfferService comboOfferService,
		SaleService saleService,
		SaleRepository saleRepository,
		SaleDetailRepository saleDetailRepository,
		SaleComboLineRepository saleComboLineRepository
	) {
		this.companyRepository = companyRepository;
		this.platformCompanyService = platformCompanyService;
		this.inventoryService = inventoryService;
		this.branchRepository = branchRepository;
		this.supplierRepository = supplierRepository;
		this.productPresentationRepository = productPresentationRepository;
		this.purchaseService = purchaseService;
		this.purchaseDetailRepository = purchaseDetailRepository;
		this.inventoryLayerRepository = inventoryLayerRepository;
		this.inventoryBalanceRepository = inventoryBalanceRepository;
		this.stockMovementRepository = stockMovementRepository;
		this.userRepository = userRepository;
		this.userService = userService;
		this.userProfileService = userProfileService;
		this.passwordEncoder = passwordEncoder;
		this.objectMapper = objectMapper;
		this.comboOfferService = comboOfferService;
		this.saleService = saleService;
		this.saleRepository = saleRepository;
		this.saleDetailRepository = saleDetailRepository;
		this.saleComboLineRepository = saleComboLineRepository;
	}

	@Test
	void contextLoads() {
	}

	@Test
	void inventoryReadModelsAcceptEmptyOptionalFilters() {
		var company = companyRepository.findByTenantSlugAndDeletedAtIsNull("demo").orElseThrow();
		var user = companyAdmin(company.getId(), null);

		assertDoesNotThrow(() -> inventoryService.listBalances(user, 1, 20, null, null, null, null, ""));
		assertDoesNotThrow(() -> inventoryService.listLayers(user, 1, 20, null, null, null, null));
		assertDoesNotThrow(() -> inventoryService.getKardex(user, 1, 20, null, null, null, null, null, null));
		assertDoesNotThrow(() -> inventoryService.listMovements(user, 1, 20, null, null, null, null, null, null));
	}

	@Test
	void platformCompanyListSupportsOptionalFilters() {
		var company = companyRepository.findByTenantSlugAndDeletedAtIsNull("demo").orElseThrow();

		var unfiltered = assertDoesNotThrow(() -> platformCompanyService.listCompanies(1, 20, null, null));
		assertTrue(unfiltered.totalItems() > 0);

		var filtered = assertDoesNotThrow(() -> platformCompanyService.listCompanies(1, 20, "DEMO", company.getStatus()));
		assertTrue(filtered.items().stream().anyMatch(item -> item.id().equals(company.getId())));

		var noMatch = assertDoesNotThrow(() -> platformCompanyService.listCompanies(
			1,
			20,
			"missing-" + UUID.randomUUID(),
			null
		));
		assertTrue(noMatch.items().isEmpty());
	}

	@Test
	void platformCompanyCreationAcceptsMissingOrNullTaxFlag() throws Exception {
		var missingTaxFlag = objectMapper.readValue("{}", CreatePlatformCompanyRequest.class);
		var nullTaxFlag = objectMapper.readValue("{\"taxEnabled\":null}", CreatePlatformCompanyRequest.class);
		var enabledTaxFlag = objectMapper.readValue("{\"taxEnabled\":true}", CreatePlatformCompanyRequest.class);

		assertFalse(Boolean.TRUE.equals(missingTaxFlag.taxEnabled()));
		assertFalse(Boolean.TRUE.equals(nullTaxFlag.taxEnabled()));
		assertTrue(Boolean.TRUE.equals(enabledTaxFlag.taxEnabled()));
	}

	@Test
	@Transactional
	void ownPasswordChangeClearsRequirementAndRotatesAuthenticationVersion() {
		var company = companyRepository.findByTenantSlugAndDeletedAtIsNull("demo").orElseThrow();
		var user = userRepository.findCompanyAdminsByCompanyId(company.getId()).getFirst();
		var currentPassword = "Temporary123!";
		var newPassword = "Personal123!";
		int previousAuthVersion = user.getAuthVersion();
		user.setPasswordHash(passwordEncoder.encode(currentPassword));
		user.setMustChangePassword(true);
		user.setStatus(UserStatus.ACTIVE);
		user.getBranch().setActive(true);

		var authenticatedUser = companyAdmin(company.getId(), user.getBranch().getId(), user.getId());
		var session = userProfileService.changePassword(
			authenticatedUser,
			new ChangeOwnPasswordRequest(currentPassword, newPassword)
		);

		assertFalse(user.isMustChangePassword());
		assertEquals(previousAuthVersion + 1, user.getAuthVersion());
		assertTrue(passwordEncoder.matches(newPassword, user.getPasswordHash()));
		assertFalse(session.mustChangePassword());
	}

	@Test
	@Transactional
	void voidConfirmedPurchaseVoidsItsOriginalFifoLayer() {
		var company = companyRepository.findByTenantSlugAndDeletedAtIsNull("demo").orElseThrow();
		var branch = branchRepository.findAll().stream()
			.filter(candidate -> candidate.getCompany().getId().equals(company.getId()))
			.filter(candidate -> candidate.isActive() && candidate.getDeletedAt() == null)
			.findFirst()
			.orElseThrow();
		var supplier = supplierRepository.findAll().stream()
			.filter(candidate -> candidate.getCompany().getId().equals(company.getId()))
			.filter(candidate -> candidate.isActive() && candidate.getDeletedAt() == null)
			.findFirst()
			.orElseThrow();
		var presentation = productPresentationRepository.findAll().stream()
			.filter(candidate -> candidate.getCompany().getId().equals(company.getId()))
			.filter(candidate -> candidate.isActive() && candidate.getDeletedAt() == null)
			.filter(candidate -> candidate.getProduct().isActive() && candidate.getProduct().isStockable())
			.findFirst()
			.orElseThrow();
		var actor = userRepository.findAll().stream()
			.filter(candidate -> candidate.getCompany().getId().equals(company.getId()))
			.findFirst()
			.orElseThrow();
		var user = companyAdmin(company.getId(), branch.getId(), actor.getId());
		var quantity = BigDecimal.valueOf(3);
		var unitCost = BigDecimal.valueOf(7);

		var created = purchaseService.createPurchase(user, new UpsertPurchaseRequest(
			branch.getId(),
			supplier.getId(),
			"QA-VOID-" + UUID.randomUUID().toString().substring(0, 8),
			null,
			LocalDate.now(),
			PaymentMethod.CASH,
			PaymentType.CASH,
			PurchaseStatus.DRAFT,
			BigDecimal.ZERO,
			"exact void test",
			List.of(new PurchaseDetailRequest(presentation.getId(), quantity, unitCost, BigDecimal.ZERO, null, null))
		));

		var confirmed = purchaseService.confirmPurchase(user, created.id());
		var detail = purchaseDetailRepository.findByPurchase_IdOrderByCreatedAtAsc(confirmed.id()).getFirst();
		var layersAfterConfirm = inventoryLayerRepository.findAll().stream()
			.filter(layer -> detail.getId().equals(layer.getPurchaseDetailId()))
			.toList();
		assertEquals(1, layersAfterConfirm.size());
		var layerAfterConfirm = layersAfterConfirm.getFirst();
		assertEquals(InventoryLayerStatus.OPEN, layerAfterConfirm.getStatus());
		assertEquals(0, detail.getQuantityBase().compareTo(layerAfterConfirm.getQuantityRemaining()));

		var balanceAfterConfirm = inventoryBalanceRepository
			.findByCompany_IdAndBranch_IdAndLocation_IdAndProduct_Id(company.getId(), branch.getId(), layerAfterConfirm.getLocation().getId(), detail.getProduct().getId())
			.orElseThrow();
		var onHandAfterConfirm = balanceAfterConfirm.getQuantityOnHand();

		var voided = purchaseService.voidPurchase(user, created.id(), new VoidPurchaseRequest("exact void test"));

		assertEquals(PurchaseStatus.VOIDED, voided.status());
		var layerAfterVoid = inventoryLayerRepository.findAll().stream()
			.filter(layer -> detail.getId().equals(layer.getPurchaseDetailId()))
			.findFirst()
			.orElseThrow();
		assertEquals(InventoryLayerStatus.VOIDED, layerAfterVoid.getStatus());
		assertEquals(0, BigDecimal.ZERO.compareTo(layerAfterVoid.getQuantityRemaining()));
		var balanceAfterVoid = inventoryBalanceRepository
			.findByCompany_IdAndBranch_IdAndLocation_IdAndProduct_Id(company.getId(), branch.getId(), layerAfterVoid.getLocation().getId(), detail.getProduct().getId())
			.orElseThrow();
		assertEquals(0, onHandAfterConfirm.subtract(detail.getQuantityBase()).compareTo(balanceAfterVoid.getQuantityOnHand()));
		assertTrue(stockMovementRepository.findAll().stream().anyMatch(movement ->
			movement.getMovementType() == StockMovementType.PURCHASE_VOID_OUT
				&& detail.getId().equals(movement.getSourceId())
				&& detail.getProduct().getId().equals(movement.getProduct().getId())
		));
	}

	@Test
	@Transactional
	void changingUserBranchBackToPreviousBranchReusesAssignment() {
		var company = companyRepository.findByTenantSlugAndDeletedAtIsNull("demo").orElseThrow();
		var targetUser = userRepository.findAll().stream()
			.filter(candidate -> candidate.getCompany().getId().equals(company.getId()))
			.filter(candidate -> candidate.getBranch() != null)
			.findFirst()
			.orElseThrow();
		var originalBranch = targetUser.getBranch();
		var alternateBranch = new BranchEntity();
		alternateBranch.setCompany(company);
		alternateBranch.setName("QA Branch " + UUID.randomUUID().toString().substring(0, 8));
		alternateBranch.setCode("QA" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
		alternateBranch.setActive(true);
		alternateBranch = branchRepository.save(alternateBranch);
		var actor = companyAdmin(company.getId(), originalBranch.getId(), targetUser.getId());

		userService.changeUserBranch(actor, targetUser.getId(), new ChangeUserBranchRequest(alternateBranch.getId()));
		var movedBack = userService.changeUserBranch(actor, targetUser.getId(), new ChangeUserBranchRequest(originalBranch.getId()));

		assertEquals(originalBranch.getId(), movedBack.branch().id());
	}

	@Test
	@Transactional
	void comboOnlyDraftTracksComponentsAndCanBeDeleted() {
		var company = companyRepository.findByTenantSlugAndDeletedAtIsNull("demo").orElseThrow();
		var branch = branchRepository.findAll().stream()
			.filter(candidate -> candidate.getCompany().getId().equals(company.getId()))
			.filter(candidate -> candidate.isActive() && candidate.getDeletedAt() == null)
			.findFirst()
			.orElseThrow();
		var presentation = productPresentationRepository.findAll().stream()
			.filter(candidate -> candidate.getCompany().getId().equals(company.getId()))
			.filter(candidate -> candidate.isActive() && candidate.getDeletedAt() == null)
			.filter(candidate -> candidate.getProduct().isActive() && candidate.getProduct().isStockable())
			.filter(candidate -> candidate.getSalePrice().signum() > 0)
			.findFirst()
			.orElseThrow();
		var actor = userRepository.findAll().stream()
			.filter(candidate -> candidate.getCompany().getId().equals(company.getId()))
			.findFirst()
			.orElseThrow();
		var user = companyAdmin(company.getId(), branch.getId(), actor.getId());
		String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		var combo = comboOfferService.createCombo(user, new UpsertComboOfferRequest(
			"QA Combo " + suffix,
			"QA-COMBO-" + suffix,
			"QA" + suffix,
			null,
			presentation.getSalePrice(),
			Instant.now().minusSeconds(60),
			Instant.now().plusSeconds(3600),
			true,
			List.of(new ComboOfferItemRequest(
				presentation.getProduct().getId(),
				presentation.getId(),
				BigDecimal.ONE
			))
		));
		assertEquals(1, combo.items().size());

		var created = saleService.createSale(user, new UpsertSaleRequest(
			branch.getId(),
			null,
			"QA-COMBO-SALE-" + suffix,
			SalePaymentMethod.CASH,
			SaleStatus.DRAFT,
			BigDecimal.ZERO,
			"combo-only draft test",
			List.of(),
			List.of(new SaleComboLineRequest(combo.id(), BigDecimal.ONE))
		));

		assertEquals(1, created.comboLines().size());
		assertEquals(1, created.details().size());
		assertEquals(SaleDetailSourceType.COMBO_LINE, created.details().getFirst().sourceType());
		assertEquals(created.comboLines().getFirst().id(), created.details().getFirst().comboLineId());
		assertEquals(1, comboOfferService.listCombos(user, 1, 20, true, suffix).items().size());

		saleService.deleteDraftSale(user, created.id());

		assertTrue(saleRepository.findByIdAndCompany_Id(created.id(), company.getId()).isEmpty());
		assertTrue(saleDetailRepository.findBySale_IdOrderByCreatedAtAsc(created.id()).isEmpty());
		assertTrue(saleComboLineRepository.findBySale_IdOrderByCreatedAtAsc(created.id()).isEmpty());
	}

	private AuthenticatedUser companyAdmin(UUID companyId, UUID branchId) {
		return companyAdmin(companyId, branchId, companyId);
	}

	private AuthenticatedUser companyAdmin(UUID companyId, UUID branchId, UUID userId) {
		return new AuthenticatedUser(
			companyId,
			branchId,
			null,
			userId,
			"demo",
			"test",
			"COMPANY_ADMIN",
			Set.of("inventory.view", "inventory.view_kardex"),
			false,
			0,
			Instant.now().plusSeconds(300)
		);
	}

}
