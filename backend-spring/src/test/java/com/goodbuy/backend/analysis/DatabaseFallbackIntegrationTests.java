package com.goodbuy.backend.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:postgresql://127.0.0.1:1/unavailable",
		"spring.datasource.username=unavailable",
		"spring.datasource.password=unavailable",
		"spring.datasource.hikari.connection-timeout=500",
		"spring.datasource.hikari.initialization-fail-timeout=-1",
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=none",
		"goodbuy.ocr.mode=mock"
})
@AutoConfigureMockMvc
class DatabaseFallbackIntegrationTests {

	private static final byte[] PNG_CONTENT = new byte[]{
			(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01};

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void bootsAndCompletesAnalysisWhenDatabaseCannotBeReached() throws Exception {
		MockMultipartFile image = new MockMultipartFile(
				"images", "transactions.png", MediaType.IMAGE_PNG_VALUE, PNG_CONTENT);
		MockMultipartFile ownerName = new MockMultipartFile(
				"ownerName", "", MediaType.TEXT_PLAIN_VALUE, "김세빈".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/analyses").file(image).file(ownerName))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.categoryCatalogSource").value("FALLBACK"))
				.andExpect(jsonPath("$.summary.expenseAmount").value(56_430))
				.andExpect(jsonPath("$.summary.parsedAmount").value(57_680))
				.andExpect(jsonPath("$.summary.anomalyAmount").value(1_250))
				.andExpect(jsonPath("$.transactions[*].merchantType").doesNotExist())
				.andExpect(jsonPath("$.dominantCategory.purposeCategory").value("FOOD"))
				.andExpect(jsonPath("$.dominantCategory.gifUrl").value(
						"https://pojybqexgdkfomwbpnih.supabase.co/storage/v1/object/public/gif/FOOD.gif"));
	}

	@Test
	void removesIndustryFieldFromPublishedOpenApiAndBrowserTester() throws Exception {
		var result = mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk()).andReturn();
		var schema = objectMapper.readTree(result.getResponse().getContentAsByteArray());
		var fields = schema.get("components").get("schemas").get("TransactionResponse").get("properties");
		assertTrue(fields.has("purposeCategory"));
		assertTrue(fields.has("anomalyReason"));
		assertFalse(fields.has("merchantType"));

		var page = mockMvc.perform(get("/index.html"))
				.andExpect(status().isOk()).andReturn();
		assertFalse(page.getResponse().getContentAsString().contains("transaction.merchantType"));
	}
}
