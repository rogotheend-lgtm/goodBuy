package com.goodbuy.backend.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "goodbuy.web")
public record WebProperties(String allowedOrigin) {
}
