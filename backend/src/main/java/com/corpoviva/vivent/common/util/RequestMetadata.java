package com.corpoviva.vivent.common.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestMetadata {

	private RequestMetadata() {
	}

	public static String clientIp(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	public static String userAgent(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		return request.getHeader("User-Agent");
	}
}
