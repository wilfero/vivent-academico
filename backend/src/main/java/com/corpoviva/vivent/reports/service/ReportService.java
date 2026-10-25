package com.corpoviva.vivent.reports.service;

import com.corpoviva.vivent.adjustments.entity.AdjustmentStatus;
import com.corpoviva.vivent.closures.entity.CashClosureStatus;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.reports.dto.AdjustmentsReportSummaryResponse;
import com.corpoviva.vivent.reports.dto.CashClosuresReportSummaryResponse;
import com.corpoviva.vivent.reports.dto.ProductReportItemResponse;
import com.corpoviva.vivent.reports.dto.ProductsReportResponse;
import com.corpoviva.vivent.reports.dto.PurchasesReportSummaryResponse;
import com.corpoviva.vivent.reports.dto.SalesReportSummaryResponse;
import com.corpoviva.vivent.reports.dto.TransfersReportSummaryResponse;
import com.corpoviva.vivent.sales.entity.SalePaymentMethod;
import com.corpoviva.vivent.sales.entity.SaleStatus;
import com.corpoviva.vivent.purchases.entity.PurchaseStatus;
import com.corpoviva.vivent.security.AuthenticatedUser;
import com.corpoviva.vivent.transfers.entity.TransferStatus;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

	private final JdbcTemplate jdbcTemplate;
	private static final String COMPANY_ADMIN = "COMPANY_ADMIN";

	public ReportService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public SalesReportSummaryResponse salesSummary(AuthenticatedUser user, UUID branchId, UUID customerId, SaleStatus status, SalePaymentMethod paymentMethod, Instant fromDate, Instant toDate, String query) {
		List<Object> params = new ArrayList<>();
		StringBuilder where = new StringBuilder("where sale.company_id = ?\n");
		params.add(user.companyId());
		appendBranchScope(where, params, user, branchId, "sale.branch_id");
		appendUuid(where, params, "sale.customer_id", customerId);
		appendEnum(where, params, "sale.status", status);
		appendEnum(where, params, "sale.payment_method", paymentMethod);
		appendInstantFrom(where, params, "sale.sale_date", fromDate);
		appendInstantTo(where, params, "sale.sale_date", toDate);
		appendSaleQuery(where, params, query);

		String sql = """
			select count(*) as total_sales,
			       count(*) filter (where sale.status = 'CONFIRMED') as confirmed_sales,
			       count(*) filter (where sale.status = 'CANCELLED') as cancelled_sales,
			       coalesce(sum(sale.total_amount) filter (where sale.status = 'CONFIRMED'), 0) as confirmed_total,
			       coalesce(sum(sale.total_amount) filter (where sale.status = 'CANCELLED'), 0) as cancelled_total,
			       coalesce(sum(sale.discount_amount), 0) as discount_total
			from sales sale
			left join customers customer on customer.id = sale.customer_id
			""" + where;

		return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new SalesReportSummaryResponse(
			rs.getLong("total_sales"),
			rs.getLong("confirmed_sales"),
			rs.getLong("cancelled_sales"),
			rs.getBigDecimal("confirmed_total"),
			rs.getBigDecimal("cancelled_total"),
			rs.getBigDecimal("discount_total")
		), params.toArray());
	}

	@Transactional(readOnly = true)
	public PurchasesReportSummaryResponse purchasesSummary(AuthenticatedUser user, UUID branchId, UUID supplierId, PurchaseStatus status, LocalDate fromDate, LocalDate toDate, String query) {
		List<Object> params = new ArrayList<>();
		StringBuilder where = new StringBuilder("where purchase.company_id = ?\n");
		params.add(user.companyId());
		appendBranchScope(where, params, user, branchId, "purchase.branch_id");
		appendUuid(where, params, "purchase.supplier_id", supplierId);
		appendEnum(where, params, "purchase.status", status);
		appendDateFrom(where, params, "purchase.purchase_date", fromDate);
		appendDateTo(where, params, "purchase.purchase_date", toDate);
		appendPurchaseQuery(where, params, query);

		String sql = """
			select count(*) as total_purchases,
			       count(*) filter (where purchase.status = 'CONFIRMED') as confirmed_purchases,
			       count(*) filter (where purchase.status = 'VOIDED') as voided_purchases,
			       coalesce(sum(purchase.total_amount) filter (where purchase.status = 'CONFIRMED'), 0) as confirmed_total,
			       coalesce(sum(purchase.total_amount) filter (where purchase.status = 'VOIDED'), 0) as voided_total,
			       coalesce(sum(purchase.tax_amount), 0) as tax_total
			from purchases purchase
			join suppliers supplier on supplier.id = purchase.supplier_id
			""" + where;

		return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new PurchasesReportSummaryResponse(
			rs.getLong("total_purchases"),
			rs.getLong("confirmed_purchases"),
			rs.getLong("voided_purchases"),
			rs.getBigDecimal("confirmed_total"),
			rs.getBigDecimal("voided_total"),
			rs.getBigDecimal("tax_total")
		), params.toArray());
	}

	@Transactional(readOnly = true)
	public ProductsReportResponse productsReport(AuthenticatedUser user, UUID branchId, UUID categoryId, UUID productId, Instant fromDate, Instant toDate) {
		return new ProductsReportResponse(
			topSelling(user, branchId, categoryId, productId, fromDate, toDate),
			mostMoved(user, branchId, categoryId, productId, fromDate, toDate),
			noMovement(user, branchId, categoryId, productId, fromDate, toDate),
			stockStatus(user, branchId, categoryId, productId, true),
			stockStatus(user, branchId, categoryId, productId, false)
		);
	}

	@Transactional(readOnly = true)
	public TransfersReportSummaryResponse transfersSummary(AuthenticatedUser user, UUID sourceBranchId, UUID destinationBranchId, TransferStatus status, Instant fromDate, Instant toDate, String query) {
		List<Object> params = new ArrayList<>();
		StringBuilder where = new StringBuilder("where transfer.company_id = ?\n");
		params.add(user.companyId());
		appendUuid(where, params, "transfer.source_branch_id", sourceBranchId);
		appendUuid(where, params, "transfer.destination_branch_id", destinationBranchId);
		appendEnum(where, params, "transfer.status", status);
		appendInstantFrom(where, params, "transfer.requested_at", fromDate);
		appendInstantTo(where, params, "transfer.requested_at", toDate);
		appendTransferQuery(where, params, query);
		appendTransferVisibleScope(where, params, user);

		String sql = """
			select count(distinct transfer.id) as total_transfers,
			       count(distinct transfer.id) filter (where transfer.status = 'REQUESTED') as requested_transfers,
			       count(distinct transfer.id) filter (where transfer.status = 'APPROVED') as approved_transfers,
			       count(distinct transfer.id) filter (where transfer.status = 'REJECTED') as rejected_transfers,
			       count(distinct transfer.id) filter (where transfer.status = 'IN_TRANSIT') as in_transit_transfers,
			       count(distinct transfer.id) filter (where transfer.status = 'PARTIALLY_RECEIVED') as partially_received_transfers,
			       count(distinct transfer.id) filter (where transfer.status = 'RECEIVED') as received_transfers,
			       count(distinct transfer.id) filter (where transfer.status = 'CANCELLED') as cancelled_transfers,
			       count(distinct transfer.id) filter (where transfer.status in ('REQUESTED', 'APPROVED', 'IN_TRANSIT', 'PARTIALLY_RECEIVED')) as pending_transfers,
			       coalesce(sum(detail.quantity_requested_base), 0) as requested_base_total,
			       coalesce(sum(detail.quantity_approved_base), 0) as approved_base_total,
			       coalesce(sum(detail.quantity_received_base), 0) as received_base_total
			from transfers transfer
			left join transfer_details detail on detail.transfer_id = transfer.id
			""" + where;

		return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new TransfersReportSummaryResponse(
			rs.getLong("total_transfers"),
			rs.getLong("requested_transfers"),
			rs.getLong("approved_transfers"),
			rs.getLong("rejected_transfers"),
			rs.getLong("in_transit_transfers"),
			rs.getLong("partially_received_transfers"),
			rs.getLong("received_transfers"),
			rs.getLong("cancelled_transfers"),
			rs.getLong("pending_transfers"),
			nonNull(rs.getBigDecimal("requested_base_total")),
			nonNull(rs.getBigDecimal("approved_base_total")),
			nonNull(rs.getBigDecimal("received_base_total"))
		), params.toArray());
	}

	@Transactional(readOnly = true)
	public AdjustmentsReportSummaryResponse adjustmentsSummary(AuthenticatedUser user, UUID branchId, AdjustmentStatus status, Instant fromDate, Instant toDate, String query) {
		List<Object> params = new ArrayList<>();
		StringBuilder where = new StringBuilder("where adjustment.company_id = ?\n");
		params.add(user.companyId());
		appendBranchScope(where, params, user, branchId, "adjustment.branch_id");
		appendEnum(where, params, "adjustment.status", status);
		appendInstantFrom(where, params, "adjustment.created_at", fromDate);
		appendInstantTo(where, params, "adjustment.created_at", toDate);
		appendAdjustmentQuery(where, params, query);

		String sql = """
			select count(distinct adjustment.id) as total_adjustments,
			       count(distinct adjustment.id) filter (where adjustment.status = 'DRAFT') as draft_adjustments,
			       count(distinct adjustment.id) filter (where adjustment.status = 'PENDING_APPROVAL') as pending_approval_adjustments,
			       count(distinct adjustment.id) filter (where adjustment.status = 'APPROVED') as approved_adjustments,
			       count(distinct adjustment.id) filter (where adjustment.status = 'REJECTED') as rejected_adjustments,
			       count(distinct adjustment.id) filter (where adjustment.status = 'CANCELLED') as cancelled_adjustments,
			       coalesce(sum(line.quantity_base) filter (where adjustment.direction = 'IN'), 0) as quantity_in_base,
			       coalesce(sum(line.quantity_base) filter (where adjustment.direction = 'OUT'), 0) as quantity_out_base
			from inventory_adjustments adjustment
			left join inventory_adjustment_lines line on line.adjustment_id = adjustment.id
			""" + where;

		return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new AdjustmentsReportSummaryResponse(
			rs.getLong("total_adjustments"),
			rs.getLong("draft_adjustments"),
			rs.getLong("pending_approval_adjustments"),
			rs.getLong("approved_adjustments"),
			rs.getLong("rejected_adjustments"),
			rs.getLong("cancelled_adjustments"),
			nonNull(rs.getBigDecimal("quantity_in_base")),
			nonNull(rs.getBigDecimal("quantity_out_base"))
		), params.toArray());
	}

	@Transactional(readOnly = true)
	public CashClosuresReportSummaryResponse cashClosuresSummary(AuthenticatedUser user, UUID branchId, CashClosureStatus status, LocalDate fromDate, LocalDate toDate) {
		List<Object> params = new ArrayList<>();
		StringBuilder where = new StringBuilder("where closure.company_id = ?\n");
		params.add(user.companyId());
		appendBranchScope(where, params, user, branchId, "closure.branch_id");
		appendEnum(where, params, "closure.status", status);
		appendDateFrom(where, params, "closure.closure_date", fromDate);
		appendDateTo(where, params, "closure.closure_date", toDate);

		String sql = """
			select count(*) as total_closures,
			       count(*) filter (where closure.status = 'OPEN') as open_closures,
			       count(*) filter (where closure.status = 'CLOSED') as closed_closures,
			       count(*) filter (where closure.status = 'APPROVED') as approved_closures,
			       count(*) filter (where closure.status = 'REOPENED') as reopened_closures,
			       coalesce(sum(closure.expected_total), 0) as expected_total,
			       coalesce(sum(closure.counted_total), 0) as counted_total,
			       coalesce(sum(closure.difference_amount), 0) as difference_total
			from cash_closures closure
			""" + where;

		return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new CashClosuresReportSummaryResponse(
			rs.getLong("total_closures"),
			rs.getLong("open_closures"),
			rs.getLong("closed_closures"),
			rs.getLong("approved_closures"),
			rs.getLong("reopened_closures"),
			nonNull(rs.getBigDecimal("expected_total")),
			nonNull(rs.getBigDecimal("counted_total")),
			nonNull(rs.getBigDecimal("difference_total"))
		), params.toArray());
	}

	private List<ProductReportItemResponse> topSelling(AuthenticatedUser user, UUID branchId, UUID categoryId, UUID productId, Instant fromDate, Instant toDate) {
		List<Object> params = new ArrayList<>();
		StringBuilder where = new StringBuilder("where sale.company_id = ? and sale.status = 'CONFIRMED' and detail.source_type = 'NORMAL'\n");
		params.add(user.companyId());
		appendBranchScope(where, params, user, branchId, "sale.branch_id");
		appendUuid(where, params, "product.category_id", categoryId);
		appendUuid(where, params, "product.id", productId);
		appendInstantFrom(where, params, "sale.sale_date", fromDate);
		appendInstantTo(where, params, "sale.sale_date", toDate);
		String sql = """
			select product.id as product_id, product.name as product_name, category.name as category_name,
			       sale.branch_id as branch_id, branch.name as branch_name, product.base_unit_code,
			       coalesce(sum(detail.quantity_base), 0) as quantity,
			       coalesce(sum(detail.total_amount), 0) as amount,
			       count(*) as movement_count
			from sale_details detail
			join sales sale on sale.id = detail.sale_id
			join products product on product.id = detail.product_id
			left join categories category on category.id = product.category_id
			join branches branch on branch.id = sale.branch_id
			""" + where + """
			group by product.id, product.name, category.name, sale.branch_id, branch.name, product.base_unit_code
			order by quantity desc
			limit 10
			""";
		return jdbcTemplate.query(sql, (rs, rowNum) -> productItem(rs), params.toArray());
	}

	private List<ProductReportItemResponse> mostMoved(AuthenticatedUser user, UUID branchId, UUID categoryId, UUID productId, Instant fromDate, Instant toDate) {
		List<Object> params = new ArrayList<>();
		StringBuilder where = new StringBuilder("where movement.company_id = ?\n");
		params.add(user.companyId());
		appendBranchScope(where, params, user, branchId, "movement.branch_id");
		appendUuid(where, params, "product.category_id", categoryId);
		appendUuid(where, params, "product.id", productId);
		appendInstantFrom(where, params, "movement.created_at", fromDate);
		appendInstantTo(where, params, "movement.created_at", toDate);
		String sql = """
			select product.id as product_id, product.name as product_name, category.name as category_name,
			       movement.branch_id as branch_id, branch.name as branch_name, product.base_unit_code,
			       coalesce(sum(movement.quantity_in + movement.quantity_out), 0) as quantity,
			       coalesce(sum(coalesce(movement.total_cost, 0)), 0) as amount,
			       count(*) as movement_count
			from stock_movements movement
			join products product on product.id = movement.product_id
			left join categories category on category.id = product.category_id
			join branches branch on branch.id = movement.branch_id
			""" + where + """
			group by product.id, product.name, category.name, movement.branch_id, branch.name, product.base_unit_code
			order by movement_count desc, quantity desc
			limit 10
			""";
		return jdbcTemplate.query(sql, (rs, rowNum) -> productItem(rs), params.toArray());
	}

	private List<ProductReportItemResponse> noMovement(AuthenticatedUser user, UUID branchId, UUID categoryId, UUID productId, Instant fromDate, Instant toDate) {
		List<Object> params = new ArrayList<>();
		StringBuilder where = new StringBuilder("where product.company_id = ? and product.deleted_at is null\n");
		params.add(user.companyId());
		appendUuid(where, params, "product.category_id", categoryId);
		appendUuid(where, params, "product.id", productId);
		List<Object> movementParams = new ArrayList<>();
		StringBuilder movementWhere = new StringBuilder("movement.company_id = product.company_id and movement.product_id = product.id");
		UUID scopedBranchId = scopedBranchId(user, branchId);
		if (scopedBranchId != null) {
			movementWhere.append(" and movement.branch_id = ?");
			movementParams.add(scopedBranchId);
		}
		appendInstantFromExists(movementWhere, movementParams, "movement.created_at", fromDate);
		appendInstantToExists(movementWhere, movementParams, "movement.created_at", toDate);
		params.addAll(movementParams);
		String sql = """
			select product.id as product_id, product.name as product_name, category.name as category_name,
			       null::uuid as branch_id, null::varchar as branch_name, product.base_unit_code,
			       0::numeric as quantity, 0::numeric as amount, 0::bigint as movement_count
			from products product
			left join categories category on category.id = product.category_id
			""" + where + " and not exists (select 1 from stock_movements movement where " + movementWhere + ") order by product.name limit 10";
		return jdbcTemplate.query(sql, (rs, rowNum) -> productItem(rs), params.toArray());
	}

	private List<ProductReportItemResponse> stockStatus(AuthenticatedUser user, UUID branchId, UUID categoryId, UUID productId, boolean lowStock) {
		List<Object> params = new ArrayList<>();
		StringBuilder where = new StringBuilder("where balance.company_id = ?\n");
		params.add(user.companyId());
		appendBranchScope(where, params, user, branchId, "balance.branch_id");
		appendUuid(where, params, "product.category_id", categoryId);
		appendUuid(where, params, "product.id", productId);
		where.append(lowStock
			? "and product.minimum_stock > 0 and balance.quantity_available > 0 and balance.quantity_available <= product.minimum_stock\n"
			: "and balance.quantity_available <= 0\n");
		String sql = """
			select product.id as product_id, product.name as product_name, category.name as category_name,
			       balance.branch_id as branch_id, branch.name as branch_name, product.base_unit_code,
			       balance.quantity_available as quantity, 0::numeric as amount, 0::bigint as movement_count
			from inventory_balances balance
			join products product on product.id = balance.product_id
			left join categories category on category.id = product.category_id
			join branches branch on branch.id = balance.branch_id
			""" + where + "order by balance.quantity_available asc limit 10";
		return jdbcTemplate.query(sql, (rs, rowNum) -> productItem(rs), params.toArray());
	}

	private ProductReportItemResponse productItem(java.sql.ResultSet rs) throws java.sql.SQLException {
		return new ProductReportItemResponse(
			rs.getObject("product_id", UUID.class),
			rs.getString("product_name"),
			rs.getString("category_name"),
			rs.getObject("branch_id", UUID.class),
			rs.getString("branch_name"),
			rs.getString("base_unit_code"),
			nonNull(rs.getBigDecimal("quantity")),
			nonNull(rs.getBigDecimal("amount")),
			rs.getLong("movement_count")
		);
	}

	private void appendBranchScope(StringBuilder where, List<Object> params, AuthenticatedUser user, UUID branchId, String column) {
		UUID scopedBranchId = scopedBranchId(user, branchId);
		appendUuid(where, params, column, scopedBranchId);
	}

	private void appendTransferVisibleScope(StringBuilder where, List<Object> params, AuthenticatedUser user) {
		if (COMPANY_ADMIN.equals(user.roleCode())) return;
		where.append("and (transfer.source_branch_id = ? or transfer.destination_branch_id = ?)\n");
		params.add(user.branchId());
		params.add(user.branchId());
	}

	private UUID scopedBranchId(AuthenticatedUser user, UUID requestedBranchId) {
		if (COMPANY_ADMIN.equals(user.roleCode())) {
			return requestedBranchId;
		}
		if (requestedBranchId != null && !user.branchId().equals(requestedBranchId)) {
			throw ApiException.forbidden(ErrorCode.PERMISSION_DENIED, "User cannot access another branch");
		}
		return user.branchId();
	}

	private void appendUuid(StringBuilder where, List<Object> params, String column, UUID value) {
		if (value == null) return;
		where.append("and ").append(column).append(" = ?\n");
		params.add(value);
	}

	private void appendEnum(StringBuilder where, List<Object> params, String column, Enum<?> value) {
		if (value == null) return;
		where.append("and ").append(column).append(" = ?\n");
		params.add(value.name());
	}

	private void appendInstantFrom(StringBuilder where, List<Object> params, String column, Instant value) {
		if (value == null) return;
		where.append("and ").append(column).append(" >= ?\n");
		params.add(Timestamp.from(value));
	}

	private void appendInstantTo(StringBuilder where, List<Object> params, String column, Instant value) {
		if (value == null) return;
		where.append("and ").append(column).append(" <= ?\n");
		params.add(Timestamp.from(value));
	}

	private void appendInstantFromExists(StringBuilder where, List<Object> params, String column, Instant value) {
		if (value == null) return;
		where.append(" and ").append(column).append(" >= ?");
		params.add(Timestamp.from(value));
	}

	private void appendInstantToExists(StringBuilder where, List<Object> params, String column, Instant value) {
		if (value == null) return;
		where.append(" and ").append(column).append(" <= ?");
		params.add(Timestamp.from(value));
	}

	private void appendDateFrom(StringBuilder where, List<Object> params, String column, LocalDate value) {
		if (value == null) return;
		where.append("and ").append(column).append(" >= ?\n");
		params.add(Date.valueOf(value));
	}

	private void appendDateTo(StringBuilder where, List<Object> params, String column, LocalDate value) {
		if (value == null) return;
		where.append("and ").append(column).append(" <= ?\n");
		params.add(Date.valueOf(value));
	}

	private void appendSaleQuery(StringBuilder where, List<Object> params, String query) {
		String trimmed = trimToNull(query);
		if (trimmed == null) return;
		where.append("and (lower(sale.sale_number) like lower(?) or lower(customer.name) like lower(?))\n");
		String pattern = "%" + trimmed + "%";
		params.add(pattern);
		params.add(pattern);
	}

	private void appendPurchaseQuery(StringBuilder where, List<Object> params, String query) {
		String trimmed = trimToNull(query);
		if (trimmed == null) return;
		where.append("and (lower(purchase.purchase_number) like lower(?) or lower(coalesce(purchase.supplier_invoice_number, '')) like lower(?) or lower(supplier.name) like lower(?))\n");
		String pattern = "%" + trimmed + "%";
		params.add(pattern);
		params.add(pattern);
		params.add(pattern);
	}

	private void appendTransferQuery(StringBuilder where, List<Object> params, String query) {
		String trimmed = trimToNull(query);
		if (trimmed == null) return;
		where.append("and lower(transfer.transfer_number) like lower(?)\n");
		params.add("%" + trimmed + "%");
	}

	private void appendAdjustmentQuery(StringBuilder where, List<Object> params, String query) {
		String trimmed = trimToNull(query);
		if (trimmed == null) return;
		where.append("and lower(adjustment.adjustment_number) like lower(?)\n");
		params.add("%" + trimmed + "%");
	}

	private String trimToNull(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private BigDecimal nonNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
