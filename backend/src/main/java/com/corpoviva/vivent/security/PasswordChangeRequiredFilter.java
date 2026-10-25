package com.corpoviva.vivent.security;

import com.corpoviva.vivent.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

	private static final String AUTH_ME_PATH = "/api/v1/auth/me";
	private static final String USER_PROFILE_PATH = "/api/v1/users/me";
	private static final String USER_PASSWORD_PATH = "/api/v1/users/me/password";

	private final SecurityResponseWriter securityResponseWriter;

	public PasswordChangeRequiredFilter(SecurityResponseWriter securityResponseWriter) {
		this.securityResponseWriter = securityResponseWriter;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)
			|| !user.mustChangePassword() || isPasswordChangeRequest(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		securityResponseWriter.write(
			response,
			HttpStatus.FORBIDDEN,
			"Password change is required before continuing",
			ErrorCode.PASSWORD_CHANGE_REQUIRED
		);
	}

	private boolean isPasswordChangeRequest(HttpServletRequest request) {
		String path = request.getServletPath();
		return (HttpMethod.GET.matches(request.getMethod()) && (AUTH_ME_PATH.equals(path) || USER_PROFILE_PATH.equals(path)))
			|| (HttpMethod.POST.matches(request.getMethod()) && USER_PASSWORD_PATH.equals(path));
	}
}
