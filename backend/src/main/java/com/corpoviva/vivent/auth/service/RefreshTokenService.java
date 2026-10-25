package com.corpoviva.vivent.auth.service;

import com.corpoviva.vivent.auth.entity.RefreshTokenEntity;
import com.corpoviva.vivent.auth.repository.RefreshTokenRepository;
import com.corpoviva.vivent.common.exception.ApiException;
import com.corpoviva.vivent.common.exception.ErrorCode;
import com.corpoviva.vivent.users.entity.UserEntity;
import com.corpoviva.vivent.users.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, validates, rotates and revokes opaque refresh tokens. Raw tokens are
 * returned to the client once; only their SHA-256 hash is persisted.
 */
@Service
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private final RefreshTokenProperties properties;
	private final SecureRandom secureRandom = new SecureRandom();

	public RefreshTokenService(
		RefreshTokenRepository refreshTokenRepository,
		UserRepository userRepository,
		RefreshTokenProperties properties
	) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.userRepository = userRepository;
		this.properties = properties;
	}

	@Transactional
	public IssuedRefreshToken issue(UserEntity user, String ipAddress, String userAgent) {
		String rawToken = generateRawToken();
		Instant now = Instant.now();
		Instant expiresAt = now.plus(properties.ttl());

		RefreshTokenEntity entity = new RefreshTokenEntity();
		entity.setCompanyId(user.getCompany().getId());
		entity.setUserId(user.getId());
		entity.setTokenHash(hash(rawToken));
		entity.setExpiresAt(expiresAt);
		entity.setIpAddress(truncate(ipAddress, 45));
		entity.setUserAgent(truncate(userAgent, 255));
		refreshTokenRepository.save(entity);

		return new IssuedRefreshToken(rawToken, expiresAt);
	}

	public record IssuedRefreshToken(String refreshToken, Instant expiresAt) {
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public UserEntity consume(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw ApiException.unauthorized(ErrorCode.INVALID_TOKEN, "Refresh token is required");
		}

		RefreshTokenEntity entity = refreshTokenRepository.findByTokenHash(hash(rawToken))
			.orElseThrow(() -> ApiException.unauthorized(ErrorCode.INVALID_TOKEN, "Invalid refresh token"));

		Instant now = Instant.now();

		if (entity.isRevoked()) {
			throw ApiException.unauthorized(ErrorCode.INVALID_TOKEN, "Refresh token was revoked");
		}

		if (entity.isExpiredAt(now)) {
			throw ApiException.unauthorized(ErrorCode.INVALID_TOKEN, "Refresh token expired");
		}

		// Rotation: the presented token can only be used once.
		entity.setRevokedAt(now);
		refreshTokenRepository.save(entity);

		UserEntity user = userRepository.findWithAssociationsById(entity.getUserId())
			.orElseThrow(() -> ApiException.unauthorized(ErrorCode.INVALID_TOKEN, "Refresh token user is not available"));

		return user;
	}

	@Transactional
	public void revoke(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}

		refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(entity -> {
			if (entity.isRevoked()) {
				return;
			}
			entity.setRevokedAt(Instant.now());
			refreshTokenRepository.save(entity);
		});
	}

	@Transactional
	public void revokeAllForUser(UUID userId) {
		refreshTokenRepository.revokeAllForUser(userId, Instant.now());
	}

	private String generateRawToken() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	private String truncate(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}
}
