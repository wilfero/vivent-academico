package com.corpoviva.vivent.security;

import com.corpoviva.vivent.auth.service.AuthService;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final AuthService authService;
	private final SecurityResponseWriter securityResponseWriter;

	public JwtAuthenticationFilter(AuthService authService, SecurityResponseWriter securityResponseWriter) {
		this.authService = authService;
		this.securityResponseWriter = securityResponseWriter;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		if (SecurityContextHolder.getContext().getAuthentication() != null) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authorizationHeader.substring(7);

		try {
			AuthenticatedUser authenticatedUser = authService.authenticateToken(token);

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				authenticatedUser,
				token,
				authenticatedUser.authorities()
			);

			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		} catch (ApiException exception) {
			SecurityContextHolder.clearContext();
			securityResponseWriter.write(response, exception.getStatus(), exception.getMessage(), exception.getErrorCode());
		} catch (RuntimeException exception) {
			SecurityContextHolder.clearContext();
			securityResponseWriter.write(response, org.springframework.http.HttpStatus.UNAUTHORIZED,
				"Invalid or expired token", ErrorCode.INVALID_TOKEN);
		}
	}
}
