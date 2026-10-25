package com.corpoviva.vivent.audit.service;

import com.corpoviva.vivent.audit.dto.AuditLogResponse;
import com.corpoviva.vivent.audit.entity.AuditAction;
import com.corpoviva.vivent.common.dto.PageResponse;
import com.corpoviva.vivent.common.util.PageRequestFactory;
import com.corpoviva.vivent.security.AuthenticatedUser;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {
	private static final String COMPANY_ADMIN = "COMPANY_ADMIN";

	private final JdbcTemplate jdbcTemplate;

	public AuditLogService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	public void log(AuthenticatedUser user, AuditAction action, String entityType, UUID entityId, String oldValue, String newValue) {
		log(user, user.branchId(), action, entityType, entityId, oldValue, newValue);
	}

	@Transactional
	public void log(AuthenticatedUser user, UUID branchId, AuditAction action, String entityType, UUID entityId, String oldValue, String newValue) {
		jdbcTemplate.update(
			"""
			insert into audit_logs (company_id, branch_id, user_id, action, entity_type, entity_id, old_value, new_value, timezone_snapshot)
			values (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, (select coalesce(timezone, 'UTC') from companies where id = ?))
			""",
			user.companyId(),
			branchId,
			user.userId(),
			action.name(),
			entityType,
			entityId,
			nullToJson(oldValue),
			nullToJson(newValue),
			user.companyId()
		);
	}

	@Transactional(readOnly = true)
	public PageResponse<AuditLogResponse> listLogs(AuthenticatedUser user, int page, int pageSize, String entityType, UUID entityId) {
		int normalizedPage = PageRequestFactory.normalizePage(page);
		int normalizedPageSize = PageRequestFactory.normalizePageSize(pageSize);
		int offset = (normalizedPage - 1) * normalizedPageSize;
		String normalizedEntityType = trimToNull(entityType);
		List<Object> filters = new ArrayList<>();
		String whereClause = buildWhereClause(user, normalizedEntityType, entityId, filters);
		Long total = jdbcTemplate.queryForObject(
			"select count(*) from audit_logs " + whereClause,
			Long.class,
			filters.toArray()
		);

		List<Object> queryParameters = new ArrayList<>(filters);
		queryParameters.add(normalizedPageSize);
		queryParameters.add(offset);
		List<AuditLogResponse> items = jdbcTemplate.query(
			""" 
			select id, company_id, branch_id, user_id, action, entity_type, entity_id,
			       old_value::text, new_value::text, ip_address::text, user_agent, timezone_snapshot, created_at
			from audit_logs
			""" + whereClause + """
			order by created_at desc
			limit ? offset ?
			""",
			(rs, rowNum) -> mapRow(rs),
			queryParameters.toArray()
		);

		long totalItems = total == null ? 0 : total;
		int totalPages = (int) Math.ceil((double) totalItems / normalizedPageSize);
		return new PageResponse<>(items, normalizedPage, normalizedPageSize, totalItems, totalPages);
	}

	private String buildWhereClause(AuthenticatedUser user, String entityType, UUID entityId, List<Object> parameters) {
		StringBuilder whereClause = new StringBuilder("where company_id = ?\n");
		parameters.add(user.companyId());

		if (!COMPANY_ADMIN.equals(user.roleCode())) {
			whereClause.append("and branch_id = ?\n");
			parameters.add(user.branchId());
		}

		if (entityType != null) {
			whereClause.append("and entity_type = ?\n");
			parameters.add(entityType);
		}

		if (entityId != null) {
			whereClause.append("and entity_id = ?\n");
			parameters.add(entityId);
		}

		return whereClause.toString();
	}

	private AuditLogResponse mapRow(ResultSet rs) throws SQLException {
		return new AuditLogResponse(
			rs.getObject("id", UUID.class),
			rs.getObject("company_id", UUID.class),
			rs.getObject("branch_id", UUID.class),
			rs.getObject("user_id", UUID.class),
			AuditAction.valueOf(rs.getString("action")),
			rs.getString("entity_type"),
			rs.getObject("entity_id", UUID.class),
			rs.getString("old_value"),
			rs.getString("new_value"),
			rs.getString("ip_address"),
			rs.getString("user_agent"),
			rs.getString("timezone_snapshot"),
			rs.getTimestamp("created_at").toInstant()
		);
	}

	private String nullToJson(String value) {
		return value == null ? null : value;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
