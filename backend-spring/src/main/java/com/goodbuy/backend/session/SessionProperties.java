package com.goodbuy.backend.session;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "goodbuy.session")
public record SessionProperties(
		String cookieName,
		Duration maxAge,
		boolean secure) {
}
