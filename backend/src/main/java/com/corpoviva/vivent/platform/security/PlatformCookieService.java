package com.corpoviva.vivent.platform.security;

import com.corpoviva.vivent.platform.config.PlatformProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PlatformCookieService {

	private static final String COOKIE_PATH = "/api/v1/platform";

	private final PlatformProperties platformProperties;

	public PlatformCookieService(PlatformProperties platformProperties) {
		this.platformProperties = platformProperties;
	}

	public String readSessionToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();

		if (cookies == null) {
			return null;
		}

		for (Cookie cookie : cookies) {
			if (platformProperties.cookieName().equals(cookie.getName())) {
				return StringUtils.hasText(cookie.getValue()) ? cookie.getValue() : null;
			}
		}

		return null;
	}

	public void writeSessionCookie(HttpServletResponse response, String token) {
		ResponseCookie cookie = baseCookie()
			.value(token)
			.maxAge(platformProperties.sessionTtl())
			.build();

		response.addHeader("Set-Cookie", cookie.toString());
	}

	public void clearSessionCookie(HttpServletResponse response) {
		ResponseCookie cookie = baseCookie()
			.value("")
			.maxAge(Duration.ZERO)
			.build();

		response.addHeader("Set-Cookie", cookie.toString());
	}

	private ResponseCookie.ResponseCookieBuilder baseCookie() {
		return ResponseCookie.from(platformProperties.cookieName(), "")
			.httpOnly(true)
			.secure(platformProperties.cookieSecure())
			.sameSite("Strict")
			.path(COOKIE_PATH);
	}
}
