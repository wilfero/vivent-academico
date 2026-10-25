package com.corpoviva.vivent.platform.security;

import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.platform.auth.service.PlatformAuthService;
import com.corpoviva.vivent.security.SecurityResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PlatformAuthenticationFilter extends OncePerRequestFilter {

	private final PlatformAuthService platformAuthService;
	private final PlatformCookieService platformCookieService;
	private final SecurityResponseWriter securityResponseWriter;

	public PlatformAuthenticationFilter(
		PlatformAuthService platformAuthService,
		PlatformCookieService platformCookieService,
		SecurityResponseWriter securityResponseWriter
	) {
		this.platformAuthService = platformAuthService;
		this.platformCookieService = platformCookieService;
		this.securityResponseWriter = securityResponseWriter;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		String token = platformCookieService.readSessionToken(request);

		if (token == null) {
			filterChain.doFilter(request, response);
			return;
		}

		if (SecurityContextHolder.getContext().getAuthentication() != null) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			PlatformPrincipal principal = platformAuthService.authenticateToken(token);

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal,
				token,
				principal.authorities()
			);

			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		} catch (ApiException exception) {
			SecurityContextHolder.clearContext();
			securityResponseWriter.write(
				response,
				exception.getStatus(),
				exception.getMessage(),
				exception.getErrorCode()
			);
		} catch (RuntimeException exception) {
			SecurityContextHolder.clearContext();
			securityResponseWriter.write(
				response,
				HttpStatus.UNAUTHORIZED,
				"Invalid or expired platform session",
				ErrorCode.AUTHENTICATION_REQUIRED
			);
		}
	}
}
