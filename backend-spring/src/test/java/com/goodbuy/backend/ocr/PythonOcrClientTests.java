package com.goodbuy.backend.ocr;

import com.goodbuy.backend.ocr.client.OcrClientProperties;
import com.goodbuy.backend.ocr.client.OcrServiceException;
import com.goodbuy.backend.ocr.client.PythonOcrClient;
import com.goodbuy.backend.ocr.dto.OcrParsedResponse;
import com.goodbuy.backend.ocr.validation.OcrResponseValidator;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PythonOcrClientTests {

	private MockRestServiceServer server;
	private PythonOcrClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		OcrClientProperties properties = new OcrClientProperties(
				"http://python-ocr",
				"/ocr/extraction",
				Duration.ofSeconds(3),
				Duration.ofSeconds(15));
		OcrResponseValidator validator = new OcrResponseValidator(
				Validation.buildDefaultValidatorFactory().getValidator());
		client = new PythonOcrClient(
				builder.baseUrl(properties.baseUrl()).build(),
				properties,
				validator);
	}

	@Test
	void sendsImageAndParsesPythonResponse() {
		String responseJson = """
				{
				  "transactions": [{"counterparty": "벌크커피", "amount": 3000}],
				  "summary": {"total_count": 1, "total_amount": 3000}
				}
				""";
		server.expect(once(), requestTo("http://python-ocr/ocr/extraction"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

		OcrParsedResponse response = client.parse(
				new OcrRequest("transactions.png", "image/png", new byte[]{1, 2, 3}));

		assertEquals(1, response.transactions().size());
		assertEquals(3_000, response.summary().totalAmount());
		server.verify();
	}

	@Test
	void mapsPythonServerFailureToOcrServiceException() {
		server.expect(once(), requestTo("http://python-ocr/ocr/extraction"))
				.andRespond(withServerError());

		assertThrows(
				OcrServiceException.class,
				() -> client.parse(new OcrRequest("transactions.png", "image/png", new byte[]{1})));
		server.verify();
	}
}
