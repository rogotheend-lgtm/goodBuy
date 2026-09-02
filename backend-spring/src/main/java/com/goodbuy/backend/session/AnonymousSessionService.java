package com.goodbuy.backend.session;

import com.goodbuy.backend.common.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AnonymousSessionService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final AnonymousSessionRepository repository;
	private final SessionProperties properties;
	private final Clock clock;

	@Autowired
	public AnonymousSessionService(
			AnonymousSessionRepository repository,
			SessionProperties properties) {
		this(repository, properties, Clock.systemUTC());
	}

	AnonymousSessionService(
			AnonymousSessionRepository repository,
			SessionProperties properties,
			Clock clock) {
		this.repository = repository;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public SessionResolution resolveOrCreate(String token) {
		Instant now = clock.instant();
		if (token != null && !token.isBlank()) {
			var existing = repository.findByTokenHashAndExpiresAtAfter(hash(token), now);
			if (existing.isPresent()) {
				AnonymousSessionEntity session = existing.get();
				session.touch(now, now.plus(properties.maxAge()));
				return new SessionResolution(session.getId(), token, false);
			}
		}

		String newToken = generateToken();
		AnonymousSessionEntity session = new AnonymousSessionEntity(
				UUID.randomUUID(),
				hash(newToken),
				now,
				now.plus(properties.maxAge()));
		repository.save(session);
		return new SessionResolution(session.getId(), newToken, true);
	}

	@Transactional(readOnly = true)
	public UUID requireActiveSession(String token) {
		if (token == null || token.isBlank()) {
			throw new ResourceNotFoundException("Anonymous session was not found");
		}

		return repository.findByTokenHashAndExpiresAtAfter(hash(token), clock.instant())
				.map(AnonymousSessionEntity::getId)
				.orElseThrow(() -> new ResourceNotFoundException("Anonymous session was not found"));
	}

	private String generateToken() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}
}
