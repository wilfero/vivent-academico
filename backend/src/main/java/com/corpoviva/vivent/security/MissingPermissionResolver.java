package com.corpoviva.vivent.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class MissingPermissionResolver {

	private static final Pattern AUTHORITY_EXPRESSION = Pattern.compile("has(?:Any)?Authority\\(([^)]*)\\)");
	private static final Pattern QUOTED_PERMISSION = Pattern.compile("'([^']+)'");

	public List<String> resolve(HttpServletRequest request) {
		Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);

		if (!(handler instanceof HandlerMethod handlerMethod)) {
			return List.of();
		}

		PreAuthorize preAuthorize = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), PreAuthorize.class);

		if (preAuthorize == null) {
			preAuthorize = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), PreAuthorize.class);
		}

		if (preAuthorize == null) {
			return List.of();
		}

		Set<String> grantedPermissions = currentGrantedPermissions();

		return extractPermissions(preAuthorize.value())
			.stream()
			.filter(permission -> !grantedPermissions.contains(permission))
			.toList();
	}

	public String formatMessage(List<String> permissions) {
		if (permissions.size() == 1) {
			return "Missing required permission: " + permissions.getFirst();
		}

		return "Missing required permissions: " + String.join(", ", permissions);
	}

	private Set<String> currentGrantedPermissions() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null) {
			return Set.of();
		}

		return authentication.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.toUnmodifiableSet());
	}

	private List<String> extractPermissions(String expression) {
		Matcher authorityMatcher = AUTHORITY_EXPRESSION.matcher(expression);

		return authorityMatcher.results()
			.flatMap(match -> {
				Matcher permissionMatcher = QUOTED_PERMISSION.matcher(match.group(1));
				return permissionMatcher.results().map(permissionMatch -> permissionMatch.group(1));
			})
			.distinct()
			.toList();
	}
}
