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
						.description("익명 사용자의 거래내역 이미지를 분석하고 소비 내역을 분류하는 Spring Backend API")
						.version("v1")
						.contact(new Contact().name("goodBuy Backend Team")));
	}
}
