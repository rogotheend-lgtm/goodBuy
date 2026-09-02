package com.goodbuy.backend.ocr.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OcrClientProperties.class)
public class OcrClientConfiguration {

	@Bean
	@Qualifier("pythonOcrRestClient")
	@ConditionalOnProperty(name = "goodbuy.ocr.mode", havingValue = "python")
	RestClient pythonOcrRestClient(RestClient.Builder builder, OcrClientProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());

		return builder
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.build();
	}
}
