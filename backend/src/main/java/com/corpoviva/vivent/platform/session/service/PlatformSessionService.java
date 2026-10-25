package com.corpoviva.vivent.platform.session.service;

import com.corpoviva.vivent.platform.config.PlatformProperties;
import com.corpoviva.vivent.platform.entity.PlatformSessionEntity;
import com.corpoviva.vivent.platform.repository.PlatformSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformSessionService {

	private static final int TOKEN_BYTES = 32;

	private final PlatformSessionRepository platformSessionRepository;
	private final PlatformProperties platformProperties;
	private final SecureRandom secureRandom = new SecureRandom();

	public PlatformSessionService(
		PlatformSessionRepository platformSessionRepository,
		PlatformProperties platformProperties
	) {
		this.platformSessionRepository = platformSessionRepository;
		this.platformProperties = platformProperties;
	}

	@Transactional
	public IssuedSession createSession(UUID platformUserId, String ipAddress, String userAgent) {
		String token = generateToken();
		PlatformSessionEntity session = new PlatformSessionEntity();
		session.setPlatformUserId(platformUserId);
		session.setTokenHash(hash(token));
		session.setExpiresAt(Instant.now().plus(platformProperties.sessionTtl()));
		session.setIpAddress(ipAddress);
		session.setUserAgent(userAgent);

		PlatformSessionEntity saved = platformSessionRepository.save(session);
		return new IssuedSession(token, saved.getExpiresAt());
	}

	@Transactional(readOnly = true)
	public Optional<PlatformSessionEntity> findValidSession(String token) {
		PlatformSessionEntity session = platformSessionRepository.findByTokenHash(hash(token)).orElse(null);

		if (session == null) {
			return Optional.empty();
		}

		if (session.getRevokedAt() != null) {
			return Optional.empty();
		}

		if (session.getExpiresAt().isBefore(Instant.now())) {
			return Optional.empty();
		}

		return Optional.of(session);
	}

	@Transactional
	public void revoke(String token) {
		platformSessionRepository.findByTokenHash(hash(token))
			.ifPresent(session -> {
				if (session.getRevokedAt() == null) {
					session.setRevokedAt(Instant.now());
				}
			});
	}

	public String hash(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

			StringBuilder builder = new StringBuilder(hash.length * 2);
			for (byte value : hash) {
				builder.append(String.format("%02x", value & 0xff));
			}

			return builder.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	private String generateToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public record IssuedSession(String token, Instant expiresAt) {
	}
}
