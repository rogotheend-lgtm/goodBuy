package com.goodbuy.backend.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "anonymous_session")
public class AnonymousSessionEntity {

	@Id
	private UUID id;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "last_accessed_at", nullable = false)
	private Instant lastAccessedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	protected AnonymousSessionEntity() {
	}

	public AnonymousSessionEntity(UUID id, String tokenHash, Instant now, Instant expiresAt) {
		this.id = id;
		this.tokenHash = tokenHash;
		this.createdAt = now;
		this.lastAccessedAt = now;
		this.expiresAt = expiresAt;
	}

	public UUID getId() {
		return id;
	}

	public void touch(Instant now, Instant expiresAt) {
		this.lastAccessedAt = now;
		this.expiresAt = expiresAt;
	}
}
