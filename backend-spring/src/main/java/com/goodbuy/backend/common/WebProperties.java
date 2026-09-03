package com.goodbuy.backend.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "goodbuy.web")
public record WebProperties(List<String> allowedOrigins) {
}
