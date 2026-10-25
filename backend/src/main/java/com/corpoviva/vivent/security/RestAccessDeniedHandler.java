package com.corpoviva.vivent.security;

import com.corpoviva.vivent.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private final SecurityResponseWriter securityResponseWriter;
	private final MissingPermissionResolver missingPermissionResolver;

	public RestAccessDeniedHandler(SecurityResponseWriter securityResponseWriter, MissingPermissionResolver missingPermissionResolver) {
		this.securityResponseWriter = securityResponseWriter;
		this.missingPermissionResolver = missingPermissionResolver;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
		AccessDeniedException accessDeniedException) throws IOException, ServletException {
		List<String> missingPermissions = missingPermissionResolver.resolve(request);
		String message = missingPermissions.isEmpty()
			? "Permission denied"
			: missingPermissionResolver.formatMessage(missingPermissions);

		securityResponseWriter.write(
			response,
			HttpStatus.FORBIDDEN,
			message,
			ErrorCode.PERMISSION_DENIED
		);
	}
}
