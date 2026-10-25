package com.corpoviva.vivent.tenant.repository;

import com.corpoviva.vivent.companies.entity.CompanyEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PublicTenantConfigRepository extends Repository<CompanyEntity, UUID> {

	@Query(value = """
		select c.id as companyId,
		       c.tenant_slug as tenantSlug,
		       c.commercial_name as commercialName,
		       c.logo_url as logoUrl,
		       c.primary_color as primaryColor,
		       c.secondary_color as secondaryColor,
		       c.default_theme as defaultTheme,
		       coalesce(array_to_json(c.supported_locales)::text, '[]') as supportedLocalesJson,
		       c.default_locale as defaultLocale,
		       c.currency_code as currencyCode,
		       c.timezone as timezone,
		       coalesce(current_subscription.status, 'TRIAL') as subscriptionStatus,
		       coalesce(array_to_json(plan_record.enabled_modules)::text, '[]') as enabledModulesJson
		from companies c
		left join lateral (
		    select s.status, s.plan_id
		    from subscriptions s
		    where s.company_id = c.id
		    order by s.created_at desc
		    limit 1
		) current_subscription on true
		left join plans plan_record on plan_record.id = current_subscription.plan_id
		where c.id = :companyId
		  and c.deleted_at is null
		""", nativeQuery = true)
	Optional<PublicTenantConfigProjection> findPublicTenantConfigByCompanyId(@Param("companyId") UUID companyId);
}
