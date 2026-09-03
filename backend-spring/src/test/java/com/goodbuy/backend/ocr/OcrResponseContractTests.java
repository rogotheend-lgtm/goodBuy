package com.goodbuy.backend.ocr;

import com.goodbuy.backend.PostgresIntegrationTest;
import com.goodbuy.backend.ocr.dto.OcrParsedResponse;
import com.goodbuy.backend.ocr.dto.OcrSummary;
import com.goodbuy.backend.ocr.validation.InvalidOcrResponseException;
import com.goodbuy.backend.ocr.validation.OcrResponseValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class OcrResponseContractTests extends PostgresIntegrationTest {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private OcrResponseValidator responseValidator;

	@Test
	void parsesAndValidatesProvidedPythonResponse() throws IOException {
		OcrParsedResponse response = readFixture();

		OcrSummary calculatedSummary = responseValidator.validateAndRecalculate(response);

		assertEquals(10, response.transactions().size());
		assertEquals("세븐일레븐 광주산정고려점", response.transactions().getFirst().counterparty());
		assertEquals(new OcrSummary(10, 57_680), calculatedSummary);
	}

	@Test
	void rejectsSummaryThatDoesNotMatchTransactions() throws IOException {
		OcrParsedResponse response = readFixture();
		OcrParsedResponse inconsistentResponse = new OcrParsedResponse(
				response.transactions(),
				new OcrSummary(10, 57_580));

		assertThrows(
				InvalidOcrResponseException.class,
				() -> responseValidator.validateAndRecalculate(inconsistentResponse));
	}

	@Test
	void rejectsInvalidTransactionFields() {
		String invalidJson = """
				{
				  "transactions": [{"counterparty": " ", "amount": 0}],
				  "summary": {"total_count": 1, "total_amount": 0}
				}
				""";

		OcrParsedResponse response = objectMapper.readValue(invalidJson, OcrParsedResponse.class);

		assertThrows(
				InvalidOcrResponseException.class,
				() -> responseValidator.validateAndRecalculate(response));
	}

	@Test
	void rejectsUnknownPythonResponseFields() {
		String jsonWithUnknownField = """
				{
				  "transactions": [],
				  "summary": {"total_count": 0, "total_amount": 0},
				  "unexpected": true
				}
				""";

		assertThrows(
				UnrecognizedPropertyException.class,
				() -> objectMapper.readValue(jsonWithUnknownField, OcrParsedResponse.class));
	}

	@Test
	void rejectsMissingRequiredSummary() {
		String jsonWithoutSummary = """
				{
				  "transactions": []
				}
				""";

		OcrParsedResponse response = objectMapper.readValue(jsonWithoutSummary, OcrParsedResponse.class);

		assertThrows(
				InvalidOcrResponseException.class,
				() -> responseValidator.validateAndRecalculate(response));
	}

	private OcrParsedResponse readFixture() throws IOException {
		ClassPathResource resource = new ClassPathResource("ocr/parsed-response.json");
		return objectMapper.readValue(resource.getInputStream(), OcrParsedResponse.class);
	}
}
