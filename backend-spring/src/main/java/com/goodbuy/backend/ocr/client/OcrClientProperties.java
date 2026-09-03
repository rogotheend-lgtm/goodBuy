package com.goodbuy.backend.ocr.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "goodbuy.ocr")
public record OcrClientProperties(
		String baseUrl,
		String parsePath,
		Duration connectTimeout,
		Duration readTimeout) {
}
