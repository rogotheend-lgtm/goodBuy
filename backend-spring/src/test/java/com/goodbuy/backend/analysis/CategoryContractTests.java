package com.goodbuy.backend.analysis;

import com.goodbuy.backend.analysis.api.TransactionResponse;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.service.MerchantClassifier;
import com.goodbuy.backend.analysis.service.NameNormalizer;
import com.goodbuy.backend.analysis.service.TransactionClassifier;
import com.goodbuy.backend.catalog.FallbackCategoryCatalog;
import com.goodbuy.backend.ocr.dto.OcrTransactionItem;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryContractTests {

	private static final Set<String> ALLOWED_CATEGORIES = Set.of(
			"FOOD", "TRANSPORT", "LIVING", "SHOPPING", "CULTURE_HOBBY",
			"HEALTH", "EDUCATION", "FIXED_SUBSCRIPTION", "OTHER");

	@Test
	void retainsExactlyTheNineAgreedCategoriesInEnumAndFallback() {
		assertEquals(ALLOWED_CATEGORIES, Arrays.stream(PurposeCategory.values())
				.map(Enum::name).collect(Collectors.toSet()));
		var fallback = new FallbackCategoryCatalog(new NameNormalizer());
		assertEquals(ALLOWED_CATEGORIES, fallback.mappings().stream()
				.map(mapping -> mapping.purposeCategory().name()).collect(Collectors.toSet()));
	}

	@Test
	void serializesOnlyTheAgreedTransactionFieldsForEveryClassificationBranch() {
		var normalizer = new NameNormalizer();
		var classifier = new TransactionClassifier(normalizer, new MerchantClassifier(normalizer));
		var mappings = new FallbackCategoryCatalog(normalizer).mappings();
		var mapper = new ObjectMapper();
		for (var input : new OcrTransactionItem[]{
					new OcrTransactionItem("김세빈", 50_000),
					new OcrTransactionItem("토스페이_TOSS", 630),
					new OcrTransactionItem("벌크커피", 3_000),
					new OcrTransactionItem("갈비", 40_000),
					new OcrTransactionItem("처음보는가게", 3_000)}) {
			var response = TransactionResponse.from(classifier.classify("김세빈", input, mappings));
			var json = mapper.readTree(mapper.writeValueAsString(response));
			assertEquals(Set.of("counterparty", "originalAmount", "personalAmount",
					"transactionType", "purposeCategory", "anomaly", "anomalyReason", "anomalyDetail"),
					json.propertyNames());
			assertFalse(json.has("merchantType"));
			assertTrue(ALLOWED_CATEGORIES.contains(json.get("purposeCategory").stringValue()));
			assertEquals(input.amount(), json.get("originalAmount").longValue());
		}
	}
}
