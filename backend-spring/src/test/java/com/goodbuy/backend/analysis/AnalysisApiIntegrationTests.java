package com.goodbuy.backend.analysis;

import com.goodbuy.backend.PostgresIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
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
		assertNotNull(document.get("paths").get("/api/v1/analyses/{analysisId}").get("get"));
		assertNotNull(document.get("paths").get("/api/v1/transactions/{transactionId}").get("patch"));
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
		assertTrue(html.contains("/api/v1/analyses/{analysisId}"));
		assertTrue(html.contains("/internal/v1/ocr/parse"));
	}

	@Test
	void analyzesImagePersistsResultAndAllowsReviewWithinSameAnonymousSession() throws Exception {
		MvcResult analysisResult = createAnalysis();
		Cookie sessionCookie = analysisResult.getResponse().getCookie("goodbuy_session");
		assertNotNull(sessionCookie);

		JsonNode response = objectMapper.readTree(analysisResult.getResponse().getContentAsByteArray());
		String analysisId = response.get("analysisId").stringValue();
		String reviewTransactionId = response.get("transactions").get(6).get("id").stringValue();

		mockMvc.perform(get("/api/v1/analyses/{analysisId}", analysisId).cookie(sessionCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.summary.expenseAmount").value(56_430))
				.andExpect(jsonPath("$.summary.needsReviewCount").value(2));

		String reviewJson = """
				{
				  "transactionType": "EXPENSE",
				  "purposeCategory": "FOOD",
				  "personalAmount": 630
				}
				""";
		mockMvc.perform(patch("/api/v1/transactions/{transactionId}", reviewTransactionId)
						.cookie(sessionCookie)
						.contentType(MediaType.APPLICATION_JSON)
						.content(reviewJson))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.summary.expenseAmount").value(57_060))
				.andExpect(jsonPath("$.summary.needsReviewCount").value(1));

		mockMvc.perform(get("/api/v1/analyses/{analysisId}", analysisId))
				.andExpect(status().isNotFound());
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
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.summary.parsedCount").value(20))
				.andExpect(jsonPath("$.summary.parsedAmount").value(115_360))
				.andExpect(jsonPath("$.summary.expenseCount").value(16))
				.andExpect(jsonPath("$.summary.expenseAmount").value(112_860))
				.andExpect(jsonPath("$.summary.needsReviewCount").value(4))
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

	@Test
	void preventsAnotherAnonymousSessionFromReadingAnAnalysis() throws Exception {
		MvcResult firstAnalysis = createAnalysis();
		String firstAnalysisId = objectMapper
				.readTree(firstAnalysis.getResponse().getContentAsByteArray())
				.get("analysisId")
				.stringValue();

		MvcResult secondAnalysis = createAnalysis();
		Cookie secondSessionCookie = secondAnalysis.getResponse().getCookie("goodbuy_session");
		assertNotNull(secondSessionCookie);

		mockMvc.perform(get("/api/v1/analyses/{analysisId}", firstAnalysisId)
						.cookie(secondSessionCookie))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	private MvcResult createAnalysis() throws Exception {
		MockMultipartFile image = new MockMultipartFile(
				"images", "transactions.png", MediaType.IMAGE_PNG_VALUE, PNG_CONTENT);
		MockMultipartFile ownerName = new MockMultipartFile(
				"ownerName", "", MediaType.TEXT_PLAIN_VALUE, "김세빈".getBytes(StandardCharsets.UTF_8));

		return mockMvc.perform(multipart("/api/v1/analyses").file(image).file(ownerName))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("goodbuy_session"))
				.andExpect(cookie().httpOnly("goodbuy_session", true))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.summary.parsedCount").value(10))
				.andExpect(jsonPath("$.summary.parsedAmount").value(57_680))
				.andExpect(jsonPath("$.summary.expenseCount").value(8))
				.andExpect(jsonPath("$.summary.expenseAmount").value(56_430))
				.andExpect(jsonPath("$.summary.needsReviewCount").value(2))
				.andReturn();
	}
}
