package com.goodbuy.backend.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

	@Bean
	OpenAPI goodBuyOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("goodBuy 소비 분석 API")
						.description("이미지를 한 번 요청받아 소비 분석 결과를 DB에 저장하고 이상치 상세 내용을 즉시 반환하는 Spring Backend API")
						.version("v1")
						.contact(new Contact().name("goodBuy Backend Team")));
	}
}
