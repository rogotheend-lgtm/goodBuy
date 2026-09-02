package com.goodbuy.backend.common;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(WebProperties.class)
public class WebConfiguration implements WebMvcConfigurer {

	private final WebProperties properties;

	public WebConfiguration(WebProperties properties) {
		this.properties = properties;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(properties.allowedOrigin())
				.allowedMethods("GET", "POST", "PATCH", "OPTIONS")
				.allowedHeaders("*")
				.allowCredentials(true);
	}
}
