package com.goodbuy.backend.analysis;

import com.goodbuy.backend.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisApiIntegrationTests extends PostgresIntegrationTest {

	private static final byte[] PNG_CONTENT = new byte[]{
			(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01};

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void prepareCategoryMap() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS categories (
				    category_id INTEGER PRIMARY KEY,
				    category_name VARCHAR(30) NOT NULL UNIQUE,
				    dutch_threshold INTEGER NOT NULL,
				    gif_url VARCHAR(255) NOT NULL
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS category_rules (
				    rule_id INTEGER PRIMARY KEY,
				    category_id INTEGER NOT NULL REFERENCES categories(category_id),
				    keyword VARCHAR(50) NOT NULL UNIQUE
				)
				""");
		jdbcTemplate.update("DELETE FROM category_rules");
		jdbcTemplate.update("DELETE FROM categories");
		jdbcTemplate.update("""
				INSERT INTO categories(category_id, category_name, dutch_threshold, gif_url) VALUES
				(1, 'FOOD', 30000, 'https://example.com/FOOD.gif'),
				(2, 'LIVING', 50000, 'https://example.com/LIVING.gif'),
				(3, 'SHOPPING', 100000, 'https://example.com/SHOPPING.gif'),
				(9, 'OTHER', 100000, 'https://example.com/OTHER.gif')
				""");
		jdbcTemplate.update("""
				INSERT INTO category_rules(rule_id, category_id, keyword) VALUES
				(1, 2, '세븐일레븐'),
				(2, 1, '버거앤타코'),
				(3, 1, '커피'),
				(4, 1, '맘스터치'),
				(5, 1, '칼국수'),
				(6, 2, '다이소'),
				(7, 2, '마트')
				""");
	}

	@Test
	void servesBrowserApiTesterAtRoot() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("index.html"));

		MvcResult page = mockMvc.perform(get("/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andReturn();
		String html = page.getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertTrue(html.contains("소비 분석 API 테스트"));
		assertTrue(html.contains("POST /api/v1/analyses"));
	}

	@Test
	void exposesImplementedEndpointsThroughOpenApi() throws Exception {
		MvcResult result = mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andReturn();

		JsonNode document = objectMapper.readTree(result.getResponse().getContentAsByteArray());
		assertEquals("goodBuy 소비 분석 API", document.get("info").get("title").stringValue());
		assertNotNull(document.get("paths").get("/api/v1/analyses").get("post"));
		assertNull(document.get("paths").get("/api/v1/analyses/{analysisId}"));
		assertNull(document.get("paths").get("/api/v1/transactions/{transactionId}"));
		JsonNode multipartSchema = document.get("paths")
				.get("/api/v1/analyses")
				.get("post")
				.get("requestBody")
				.get("content")
				.get(MediaType.MULTIPART_FORM_DATA_VALUE)
				.get("schema");
		assertEquals("array", multipartSchema.get("properties").get("images").get("type").stringValue());
		assertEquals("binary", multipartSchema.get("properties").get("images").get("items").get("format").stringValue());
	}

	@Test
	void servesSwaggerUi() throws Exception {
		mockMvc.perform(get("/swagger.html"))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	void servesCompactApiSummaryPage() throws Exception {
		MvcResult page = mockMvc.perform(get("/api-summary.html"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andReturn();

		String html = page.getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertTrue(html.contains("Backend API 한눈에 보기"));
		assertTrue(html.contains("외부 API 1개"));
		assertTrue(html.contains("/ocr/extraction"));
	}

	@Test
	void analyzesImageWithoutPersistingRequestData() throws Exception {
		int analysisCountBefore = countRows("analysis");
		int transactionCountBefore = countRows("expense_transaction");
		MvcResult analysisResult = createAnalysis();

		JsonNode response = objectMapper.readTree(analysisResult.getResponse().getContentAsByteArray());
		assertNull(response.get("analysisId"));
		assertEquals(56_430, response.get("summary").get("expenseAmount").longValue());
		assertEquals(2, response.get("summary").get("anomalyCount").intValue());
		assertEquals("ANOMALY", response.get("transactions").get(6).get("transactionType").stringValue());
		assertTrue(response.get("transactions").get(6).get("anomalyDetail").stringValue().contains("이상치"));
		assertEquals(analysisCountBefore, countRows("analysis"));
		assertEquals(transactionCountBefore, countRows("expense_transaction"));
	}

	@Test
	void usesCategoryMappingQueriedFromDatabase() throws Exception {
		jdbcTemplate.update("UPDATE category_rules SET category_id = 3 WHERE keyword = '다이소'");

		JsonNode response = objectMapper.readTree(createAnalysis().getResponse().getContentAsByteArray());

		assertEquals("SHOPPING", response.get("transactions").get(8).get("purposeCategory").stringValue());
	}

	@Test
	void returnsServiceUnavailableInsteadOfHangingWhenCategoryMapIsEmpty() throws Exception {
		jdbcTemplate.update("DELETE FROM category_rules");
		jdbcTemplate.update("DELETE FROM categories");
		MockMultipartFile image = new MockMultipartFile(
				"images", "transactions.png", MediaType.IMAGE_PNG_VALUE, PNG_CONTENT);
		MockMultipartFile ownerName = new MockMultipartFile(
				"ownerName", "", MediaType.TEXT_PLAIN_VALUE, "김세빈".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/analyses").file(image).file(ownerName))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("CATEGORY_CATALOG_UNAVAILABLE"))
				.andExpect(jsonPath("$.detail").value("카테고리 기준 정보를 불러오지 못했습니다. 잠시 후 다시 분석해주세요."));
	}

	@Test
	void rejectsImageWhoseContentDoesNotMatchMediaType() throws Exception {
		MockMultipartFile image = new MockMultipartFile(
				"images", "fake.png", MediaType.IMAGE_PNG_VALUE, "not-a-png".getBytes(StandardCharsets.UTF_8));
		MockMultipartFile ownerName = new MockMultipartFile(
				"ownerName", "", MediaType.TEXT_PLAIN_VALUE, "김세빈".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/analyses").file(image).file(ownerName))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void analyzesMultipleImagesSequentiallyAndCombinesTheirTransactions() throws Exception {
		MockMultipartFile firstImage = new MockMultipartFile(
				"images", "first.png", MediaType.IMAGE_PNG_VALUE, PNG_CONTENT);
		MockMultipartFile secondImage = new MockMultipartFile(
				"images", "second.png", MediaType.IMAGE_PNG_VALUE, PNG_CONTENT);
		MockMultipartFile ownerName = new MockMultipartFile(
				"ownerName", "", MediaType.TEXT_PLAIN_VALUE, "김세빈".getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(multipart("/api/v1/analyses")
						.file(firstImage)
						.file(secondImage)
						.file(ownerName))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.summary.parsedCount").value(20))
				.andExpect(jsonPath("$.summary.parsedAmount").value(115_360))
				.andExpect(jsonPath("$.summary.expenseCount").value(16))
				.andExpect(jsonPath("$.summary.expenseAmount").value(112_860))
				.andExpect(jsonPath("$.summary.anomalyCount").value(4))
				.andExpect(jsonPath("$.summary.anomalyAmount").value(2_500))
				.andExpect(jsonPath("$.transactions.length()").value(20));
	}

	@Test
	void rejectsMoreThanFiveImages() throws Exception {
		var request = multipart("/api/v1/analyses");
		for (int index = 1; index <= 6; index++) {
			request.file(new MockMultipartFile(
					"images", "transactions-" + index + ".png", MediaType.IMAGE_PNG_VALUE, PNG_CONTENT));
		}
		request.file(new MockMultipartFile(
				"ownerName", "", MediaType.TEXT_PLAIN_VALUE, "김세빈".getBytes(StandardCharsets.UTF_8)));

		mockMvc.perform(request)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	private MvcResult createAnalysis() throws Exception {
		MockMultipartFile image = new MockMultipartFile(
				"images", "transactions.png", MediaType.IMAGE_PNG_VALUE, PNG_CONTENT);
		MockMultipartFile ownerName = new MockMultipartFile(
				"ownerName", "", MediaType.TEXT_PLAIN_VALUE, "김세빈".getBytes(StandardCharsets.UTF_8));

		return mockMvc.perform(multipart("/api/v1/analyses").file(image).file(ownerName))
				.andExpect(status().isOk())
				.andExpect(result -> assertNull(result.getResponse().getCookie("goodbuy_session")))
				.andExpect(jsonPath("$.analysisId").doesNotExist())
				.andExpect(jsonPath("$.summary.parsedCount").value(10))
				.andExpect(jsonPath("$.summary.parsedAmount").value(57_680))
				.andExpect(jsonPath("$.summary.expenseCount").value(8))
				.andExpect(jsonPath("$.summary.expenseAmount").value(56_430))
				.andExpect(jsonPath("$.summary.anomalyCount").value(2))
				.andExpect(jsonPath("$.summary.anomalyAmount").value(1_250))
				.andReturn();
	}

	private int countRows(String tableName) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM " + tableName, Integer.class);
	}
}
