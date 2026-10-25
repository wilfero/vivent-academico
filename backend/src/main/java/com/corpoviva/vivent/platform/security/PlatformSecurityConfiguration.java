package com.corpoviva.vivent.platform.security;

import com.corpoviva.vivent.security.RestAccessDeniedHandler;
import com.corpoviva.vivent.security.RestAuthenticationEntryPoint;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class PlatformSecurityConfiguration {

	/**
	 * Dedicated filter chain for the SaaS master panel. It runs before the tenant JWT chain
	 * because of its higher precedence. Platform sessions use an opaque HttpOnly cookie, so CSRF is
	 * mitigated by SameSite=Strict; a dedicated CSRF token and MFA are deferred until the panel is
	 * exposed publicly.
	 */
	@Bean
	@Order(1)
	public SecurityFilterChain platformSecurityFilterChain(
		HttpSecurity http,
		PlatformAuthenticationFilter platformAuthenticationFilter,
		RestAuthenticationEntryPoint authenticationEntryPoint,
		RestAccessDeniedHandler accessDeniedHandler
	) throws Exception {
		return http
			.securityMatcher("/api/v1/platform/**")
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exceptionHandling -> exceptionHandling
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler)
			)
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/platform/auth/login").permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(platformAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	@Bean
	public FilterRegistrationBean<PlatformAuthenticationFilter> platformAuthenticationFilterRegistration(
		PlatformAuthenticationFilter platformAuthenticationFilter
	) {
		FilterRegistrationBean<PlatformAuthenticationFilter> registration = new FilterRegistrationBean<>(platformAuthenticationFilter);
		registration.setEnabled(false);
		return registration;
	}
}
