package com.corpoviva.vivent.closures.service;

import com.corpoviva.vivent.audit.entity.AuditAction;
import com.corpoviva.vivent.audit.service.AuditLogService;
import com.corpoviva.vivent.branches.entity.BranchEntity;
import com.corpoviva.vivent.branches.repository.BranchRepository;
import com.corpoviva.vivent.closures.dto.CashClosureCloseRequest;
import com.corpoviva.vivent.closures.dto.CashClosurePreviewResponse;
import com.corpoviva.vivent.closures.dto.CashClosureRequest;
import com.corpoviva.vivent.closures.dto.CashClosureResponse;
import com.corpoviva.vivent.closures.entity.CashClosureEntity;
import com.corpoviva.vivent.closures.entity.CashClosureStatus;
import com.corpoviva.vivent.closures.repository.CashClosureRepository;
import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.companies.entity.CompanyEntity;
import com.corpoviva.vivent.companies.repository.CompanyRepository;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.users.entity.UserEntity;
import com.corpoviva.vivent.users.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashClosureService {

	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final List<String> PAYMENT_METHOD_ORDER = List.of("CASH", "CARD", "TRANSFER", "CREDIT", "OTHER");

	private final CashClosureRepository cashClosureRepository;
	private final CompanyRepository companyRepository;
	private final BranchRepository branchRepository;
	private final UserRepository userRepository;
	private final AuditLogService auditLogService;
	private final JdbcTemplate jdbcTemplate;

	public CashClosureService(CashClosureRepository cashClosureRepository, CompanyRepository companyRepository, BranchRepository branchRepository, UserRepository userRepository, AuditLogService auditLogService, JdbcTemplate jdbcTemplate) {
		this.cashClosureRepository = cashClosureRepository;
		this.companyRepository = companyRepository;
		this.branchRepository = branchRepository;
		this.userRepository = userRepository;
		this.auditLogService = auditLogService;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public PageResponse<CashClosureResponse> listClosures(AuthenticatedUser user, int page, int pageSize, UUID branchId, CashClosureStatus status, LocalDate fromDate, LocalDate toDate) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		UUID scopedBranchId = scopedBranchId(user, branchId);
		Page<CashClosureEntity> closurePage = cashClosureRepository.findAll(
			closureSearch(user.companyId(), scopedBranchId, status, fromDate, toDate),
			PageRequestFactory.create(normalizedPage, normalizedPageSize, Sort.by("closureDate").descending().and(Sort.by("createdAt").descending()))
		);
		return PageResponse.from(closurePage.map(CashClosureResponse::from), normalizedPage, normalizedPageSize);
	}

	@Transactional(readOnly = true)
	public CashClosureResponse getClosure(AuthenticatedUser user, UUID closureId) {
		CashClosureEntity closure = cashClosureRepository.findByIdAndCompany_Id(closureId, user.companyId())
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Cash closure not found"));
		ensureCanAccessBranch(user, closure.getBranch().getId());
		return toFullResponse(closure);
	}

	@Transactional(readOnly = true)
	public CashClosurePreviewResponse previewClosure(AuthenticatedUser user, UUID branchId, LocalDate closureDate) {
		CompanyEntity company = loadCompany(user.companyId());
		BranchEntity branch = loadBranch(user, branchId);
		validateClosureDate(closureDate, resolveZone(company));
		SalesClosureTotals totals = salesTotals(user.companyId(), branch.getId(), user.userId(), closureDate, resolveZone(company));
		return new CashClosurePreviewResponse(
			branch.getId(),
			branch.getName(),
			closureDate,
			totals.confirmedSalesTotal(),
			totals.cancelledSalesTotal(),
			totals.discountTotal(),
			totals.expectedTotal(),
			orderedPayments(totals.paymentsByMethod())
		);
	}

	@Transactional
	public CashClosureResponse createClosure(AuthenticatedUser user, CashClosureRequest request) {
		CompanyEntity company = loadCompany(user.companyId());
		BranchEntity branch = loadBranch(user, request.branchId());
		UserEntity currentUser = userRepository.findByIdAndCompany_IdAndDeletedAtIsNull(user.userId(), user.companyId())
			.orElseThrow(() -> ApiException.notFound(ErrorCode.USER_INACTIVE, "User not found"));
		validateClosureDate(request.closureDate(), resolveZone(company));
		if (cashClosureRepository.existsByCompany_IdAndBranch_IdAndUser_IdAndClosureDate(user.companyId(), branch.getId(), currentUser.getId(), request.closureDate())) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Cash closure already exists for this user, branch and date");
		}

		Instant now = Instant.now();
		SalesClosureTotals totals = salesTotals(user.companyId(), branch.getId(), currentUser.getId(), request.closureDate(), resolveZone(company));
		BigDecimal countedTotal = request.countedTotal() == null ? totals.expectedTotal() : request.countedTotal();
		CashClosureEntity closure = new CashClosureEntity();
		closure.setCompany(company);
		closure.setBranch(branch);
		closure.setUser(currentUser);
		closure.setClosureDate(request.closureDate());
		closure.setStatus(CashClosureStatus.CLOSED);
		closure.setCountedTotal(countedTotal);
		closure.setNotes(trimToNull(request.notes()));
		closure.setTimezoneSnapshot(resolveCompanyTimezone(company));
		closure.setClosedAt(now);
		applyTotals(closure, totals, now);
		CashClosureEntity savedClosure = cashClosureRepository.saveAndFlush(closure);
		writePayments(savedClosure.getId(), totals.paymentsByMethod());
		auditLogService.log(user, branch.getId(), AuditAction.CREATE, "cash_closure", savedClosure.getId(), null, jsonStatus(savedClosure.getStatus()));
		return toFullResponse(savedClosure);
	}

	@Transactional
	public CashClosureResponse approveClosure(AuthenticatedUser user, UUID closureId) {
		CashClosureEntity closure = loadClosureForUpdate(user, closureId);
		if (closure.getStatus() != CashClosureStatus.CLOSED) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Only closed cash closures can be approved");
		}
		CashClosureStatus oldStatus = closure.getStatus();
		closure.setStatus(CashClosureStatus.APPROVED);
		closure.setApprovedBy(user.userId());
		closure.setApprovedAt(Instant.now());
		auditLogService.log(user, closure.getBranch().getId(), AuditAction.APPROVE, "cash_closure", closure.getId(), jsonStatus(oldStatus), jsonStatus(closure.getStatus()));
		return toFullResponse(closure);
	}

	@Transactional
	public CashClosureResponse reopenClosure(AuthenticatedUser user, UUID closureId) {
		CashClosureEntity closure = loadClosureForUpdate(user, closureId);
		if (closure.getStatus() != CashClosureStatus.APPROVED) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Only approved cash closures can be reopened");
		}
		CashClosureStatus oldStatus = closure.getStatus();
		recalculateTotals(closure);
		closure.setStatus(CashClosureStatus.REOPENED);
		auditLogService.log(user, closure.getBranch().getId(), AuditAction.UPDATE, "cash_closure", closure.getId(), jsonStatus(oldStatus), jsonStatus(closure.getStatus()));
		return toFullResponse(closure);
	}

	@Transactional
	public CashClosureResponse closeClosure(AuthenticatedUser user, UUID closureId, CashClosureCloseRequest request) {
		CashClosureEntity closure = loadClosureForUpdate(user, closureId);
		if (closure.getStatus() != CashClosureStatus.REOPENED) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Only reopened cash closures can be closed");
		}
		if (request != null && request.countedTotal() != null) {
			closure.setCountedTotal(request.countedTotal());
		}
		CashClosureStatus oldStatus = closure.getStatus();
		recalculateTotals(closure);
		closure.setStatus(CashClosureStatus.CLOSED);
		closure.setClosedAt(Instant.now());
		auditLogService.log(user, closure.getBranch().getId(), AuditAction.UPDATE, "cash_closure", closure.getId(), jsonStatus(oldStatus), jsonStatus(closure.getStatus()));
		return toFullResponse(closure);
	}

	private CashClosureEntity loadClosureForUpdate(AuthenticatedUser user, UUID closureId) {
		CashClosureEntity closure = cashClosureRepository.findByIdAndCompanyIdForUpdate(closureId, user.companyId())
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Cash closure not found"));
		ensureCanAccessBranch(user, closure.getBranch().getId());
		return closure;
	}

	private void recalculateTotals(CashClosureEntity closure) {
		CompanyEntity company = closure.getCompany();
		SalesClosureTotals totals = salesTotals(
			company.getId(),
			closure.getBranch().getId(),
			closure.getUser().getId(),
			closure.getClosureDate(),
			resolveZone(company)
		);
		applyTotals(closure, totals, Instant.now());
		writePayments(closure.getId(), totals.paymentsByMethod());
	}

	private void applyTotals(CashClosureEntity closure, SalesClosureTotals totals, Instant now) {
		closure.setConfirmedSalesTotal(totals.confirmedSalesTotal());
		closure.setCancelledSalesTotal(totals.cancelledSalesTotal());
		closure.setDiscountTotal(totals.discountTotal());
		closure.setExpectedTotal(totals.expectedTotal());
		closure.setDifferenceAmount(closure.getCountedTotal().subtract(totals.expectedTotal()));
		closure.setTotalsCalculatedAt(now);
	}

	private CashClosureResponse toFullResponse(CashClosureEntity closure) {
		Map<String, BigDecimal> payments = readPayments(closure.getId());
		Freshness freshness = freshness(closure);
		return CashClosureResponse.from(closure, payments, freshness.outdated(), freshness.salesAfterClosureCount());
	}

	private void writePayments(UUID closureId, Map<String, BigDecimal> paymentsByMethod) {
		jdbcTemplate.update("delete from cash_closure_payments where cash_closure_id = ?", closureId);
		for (Map.Entry<String, BigDecimal> entry : paymentsByMethod.entrySet()) {
			if (entry.getValue().signum() <= 0) continue;
			jdbcTemplate.update(
				"insert into cash_closure_payments (cash_closure_id, payment_method, amount) values (?, ?, ?)",
				closureId, entry.getKey(), entry.getValue()
			);
		}
	}

	private Map<String, BigDecimal> readPayments(UUID closureId) {
		Map<String, BigDecimal> payments = new LinkedHashMap<>();
		jdbcTemplate.query(
			"select payment_method, amount from cash_closure_payments where cash_closure_id = ? order by payment_method",
			(org.springframework.jdbc.core.RowCallbackHandler) rs -> payments.put(rs.getString("payment_method"), nonNull(rs.getBigDecimal("amount"))),
			closureId
		);
		return payments;
	}

	private Freshness freshness(CashClosureEntity closure) {
		CompanyEntity company = closure.getCompany();
		ZoneId zoneId = resolveZone(company);
		Instant from = closure.getClosureDate().atStartOfDay(zoneId).toInstant();
		Instant to = closure.getClosureDate().plusDays(1).atStartOfDay(zoneId).toInstant();
		Instant calculatedAt = closure.getTotalsCalculatedAt() != null ? closure.getTotalsCalculatedAt() : closure.getCreatedAt();
		String sql = """
			select count(*)
			from sales
			where company_id = ? and branch_id = ? and created_by = ?
			  and coalesce(confirmed_at, sale_date) >= ? and coalesce(confirmed_at, sale_date) < ?
			  and updated_at > ?
			""";
		Long count = jdbcTemplate.queryForObject(sql, Long.class,
			company.getId(),
			closure.getBranch().getId(),
			closure.getUser().getId(),
			java.sql.Timestamp.from(from),
			java.sql.Timestamp.from(to),
			java.sql.Timestamp.from(calculatedAt)
		);
		long salesAfterClosureCount = count == null ? 0L : count;
		return new Freshness(salesAfterClosureCount > 0, salesAfterClosureCount);
	}

	private SalesClosureTotals salesTotals(UUID companyId, UUID branchId, UUID userId, LocalDate closureDate, ZoneId zoneId) {
		Instant from = closureDate.atStartOfDay(zoneId).toInstant();
		Instant to = closureDate.plusDays(1).atStartOfDay(zoneId).toInstant();
		java.sql.Timestamp fromTimestamp = java.sql.Timestamp.from(from);
		java.sql.Timestamp toTimestamp = java.sql.Timestamp.from(to);

		String totalsSql = """
			select coalesce(sum(total_amount) filter (where status = 'CANCELLED'), 0) as cancelled_total,
			       coalesce(sum(discount_amount) filter (where status = 'CONFIRMED'), 0) as discount_total
			from sales
			where company_id = ? and branch_id = ? and created_by = ?
			  and coalesce(confirmed_at, sale_date) >= ? and coalesce(confirmed_at, sale_date) < ?
			""";
		ClosureTotalsRow totalsRow = jdbcTemplate.queryForObject(totalsSql, (rs, rowNum) -> new ClosureTotalsRow(
			nonNull(rs.getBigDecimal("cancelled_total")),
			nonNull(rs.getBigDecimal("discount_total"))
		), companyId, branchId, userId, fromTimestamp, toTimestamp);

		String paymentsSql = """
			select payment_method, coalesce(sum(total_amount), 0) as total
			from sales
			where company_id = ? and branch_id = ? and created_by = ?
			  and status = 'CONFIRMED'
			  and coalesce(confirmed_at, sale_date) >= ? and coalesce(confirmed_at, sale_date) < ?
			group by payment_method
			""";
		Map<String, BigDecimal> paymentsByMethod = new LinkedHashMap<>();
		jdbcTemplate.query(paymentsSql, rs -> {
			paymentsByMethod.put(rs.getString("payment_method"), nonNull(rs.getBigDecimal("total")));
		}, companyId, branchId, userId, fromTimestamp, toTimestamp);

		BigDecimal confirmedSalesTotal = paymentsByMethod.values().stream().reduce(ZERO, BigDecimal::add);
		BigDecimal expectedTotal = paymentsByMethod.getOrDefault("CASH", ZERO);
		return new SalesClosureTotals(confirmedSalesTotal, totalsRow.cancelledTotal(), totalsRow.discountTotal(), expectedTotal, paymentsByMethod);
	}

	private Map<String, BigDecimal> orderedPayments(Map<String, BigDecimal> paymentsByMethod) {
		Map<String, BigDecimal> ordered = new LinkedHashMap<>();
		for (String method : PAYMENT_METHOD_ORDER) {
			BigDecimal amount = paymentsByMethod.get(method);
			if (amount != null) ordered.put(method, amount);
		}
		return ordered;
	}

	private void validateClosureDate(LocalDate closureDate, ZoneId zoneId) {
		if (closureDate == null) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Cash closure date is required");
		}
		if (closureDate.isAfter(LocalDate.now(zoneId))) {
			throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "Cash closure date cannot be in the future");
		}
	}

	private Specification<CashClosureEntity> closureSearch(UUID companyId, UUID branchId, CashClosureStatus status, LocalDate fromDate, LocalDate toDate) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			Predicate predicate = criteriaBuilder.equal(root.get("company").get("id"), companyId);
			if (branchId != null) predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("branch").get("id"), branchId));
			if (status != null) predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
			if (fromDate != null) predicate = criteriaBuilder.and(predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("closureDate"), fromDate));
			if (toDate != null) predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThanOrEqualTo(root.get("closureDate"), toDate));
			return predicate;
		};
	}

	private CompanyEntity loadCompany(UUID companyId) {
		return companyRepository.findByIdAndDeletedAtIsNull(companyId)
			.orElseThrow(() -> ApiException.notFound(ErrorCode.TENANT_NOT_FOUND, "Company not found"));
	}

	private BranchEntity loadBranch(AuthenticatedUser user, UUID branchId) {
		ensureCanAccessBranch(user, branchId);
		return branchRepository.findByIdAndCompany_IdAndDeletedAtIsNull(branchId, user.companyId())
			.orElseThrow(() -> ApiException.notFound(ErrorCode.VALIDATION_ERROR, "Branch not found"));
	}

	private UUID scopedBranchId(AuthenticatedUser user, UUID branchId) {
		if (branchId != null) {
			ensureCanAccessBranch(user, branchId);
			return branchId;
		}
		return "COMPANY_ADMIN".equals(user.roleCode()) ? null : user.branchId();
	}

	private void ensureCanAccessBranch(AuthenticatedUser user, UUID branchId) {
		if (!"COMPANY_ADMIN".equals(user.roleCode()) && !user.branchId().equals(branchId)) {
			throw ApiException.forbidden(ErrorCode.PERMISSION_DENIED, "User cannot access this branch");
		}
	}

	private ZoneId resolveZone(CompanyEntity company) {
		return ZoneId.of(resolveCompanyTimezone(company));
	}

	private String resolveCompanyTimezone(CompanyEntity company) {
		String timezone = company.getTimezone();
		return timezone == null || timezone.isBlank() ? "UTC" : timezone;
	}

	private BigDecimal nonNull(BigDecimal value) {
		return value == null ? ZERO : value;
	}

	private String trimToNull(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String jsonStatus(CashClosureStatus status) {
		return "{\"status\":\"" + status.name() + "\"}";
	}

	private record SalesClosureTotals(BigDecimal confirmedSalesTotal, BigDecimal cancelledSalesTotal, BigDecimal discountTotal, BigDecimal expectedTotal, Map<String, BigDecimal> paymentsByMethod) {
	}

	private record ClosureTotalsRow(BigDecimal cancelledTotal, BigDecimal discountTotal) {
	}

	private record Freshness(boolean outdated, long salesAfterClosureCount) {
	}
}
