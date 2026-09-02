package com.goodbuy.backend.session;

import java.util.UUID;

public record SessionResolution(
		UUID sessionId,
		String token,
		boolean newlyCreated) {
}
