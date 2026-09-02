package com.goodbuy.backend.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AnonymousSessionRepository extends JpaRepository<AnonymousSessionEntity, UUID> {

	Optional<AnonymousSessionEntity> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);
}
