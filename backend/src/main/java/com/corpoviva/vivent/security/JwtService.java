package com.corpoviva.vivent.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final JwtProperties jwtProperties;
	private final SecretKey signingKey;

	public JwtService(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
		this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public IssuedToken issueToken(AuthenticatedUser authenticatedUser) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());
		String scope = authenticatedUser.permissions().stream().sorted().collect(Collectors.joining(" "));

		String token = Jwts.builder()
			.subject(authenticatedUser.userId().toString())
			.issuer(jwtProperties.issuer())
			.issuedAt(Date.from(issuedAt))
			.expiration(Date.from(expiresAt))
			.claim("companyId", authenticatedUser.companyId().toString())
			.claim("branchId", authenticatedUser.branchId().toString())
			.claim("branchName", authenticatedUser.branchName())
			.claim("userId", authenticatedUser.userId().toString())
			.claim("tenantSlug", authenticatedUser.tenantSlug())
			.claim("username", authenticatedUser.username())
			.claim("roleCode", authenticatedUser.roleCode())
			.claim("permissions", authenticatedUser.permissions())
			.claim("authVersion", authenticatedUser.authVersion())
			.claim("scope", scope)
			.signWith(signingKey)
			.compact();

		return new IssuedToken(token, expiresAt);
	}

	public ParsedToken parse(String token) {
		Claims claims = Jwts.parser()
			.verifyWith(signingKey)
			.requireIssuer(jwtProperties.issuer())
			.build()
			.parseSignedClaims(token)
			.getPayload();

		Integer authVersion = claims.get("authVersion", Integer.class);

		return new ParsedToken(
			UUID.fromString(claims.get("companyId", String.class)),
			UUID.fromString(claims.get("branchId", String.class)),
			claims.get("branchName", String.class),
			UUID.fromString(claims.get("userId", String.class)),
			claims.get("tenantSlug", String.class),
			claims.get("username", String.class),
			claims.get("roleCode", String.class),
			extractPermissions(claims.get("permissions")),
			authVersion == null ? 0 : authVersion,
			claims.getExpiration().toInstant()
		);
	}

	private Set<String> extractPermissions(Object permissionsClaim) {
		if (!(permissionsClaim instanceof Collection<?> collection)) {
			return Set.of();
		}

		return collection.stream()
			.map(String::valueOf)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public record IssuedToken(String token, Instant expiresAt) {
	}

	public record ParsedToken(
		UUID companyId,
		UUID branchId,
		String branchName,
		UUID userId,
		String tenantSlug,
		String username,
		String roleCode,
		Set<String> permissions,
		int authVersion,
		Instant expiresAt
	) {
	}
}
