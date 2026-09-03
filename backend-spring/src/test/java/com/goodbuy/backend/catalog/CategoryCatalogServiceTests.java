package com.goodbuy.backend.catalog;

import com.goodbuy.backend.analysis.domain.AnomalyReason;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.service.MerchantClassifier;
import com.goodbuy.backend.analysis.service.NameNormalizer;
import com.goodbuy.backend.analysis.service.TransactionClassifier;
import com.goodbuy.backend.catalog.persistence.CategoryMappingRow;
import com.goodbuy.backend.catalog.persistence.CategoryRuleReadRepository;
import com.goodbuy.backend.ocr.dto.OcrTransactionItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryCatalogServiceTests {

	@Test
	void passesDatabaseGifAndDutchThresholdToAnalysisRules() {
		CategoryRuleReadRepository repository = mock(CategoryRuleReadRepository.class);
		when(repository.findAllCategoryMappings()).thenReturn(List.of(
				row(1, "커피", 1, "FOOD", 30_000, "https://example.com/FOOD.gif"),
				row(null, null, 9, "OTHER", 100_000, "https://example.com/OTHER.gif")));
		NameNormalizer normalizer = new NameNormalizer();
		CategoryCatalogService service = new CategoryCatalogService(
				repository,
				normalizer,
				new FallbackCategoryCatalog(normalizer));

		CategoryCatalogSnapshot catalog = service.loadCatalog();
		CategoryMapping food = catalog.mappings().stream()
				.filter(mapping -> mapping.purposeCategory() == PurposeCategory.FOOD)
				.findFirst()
				.orElseThrow();
		var transaction = new TransactionClassifier(normalizer, new MerchantClassifier(normalizer))
				.classify("이명로", new OcrTransactionItem("메가커피", 30_001), catalog.mappings());

		assertEquals(CategoryCatalogSource.DATABASE, catalog.source());
		assertEquals(30_000, food.dutchThreshold());
		assertEquals("https://example.com/FOOD.gif", food.gifUrl());
		assertEquals(AnomalyReason.GROUP_PAYMENT_CANDIDATE, transaction.anomalyReason());
		assertTrue(transaction.anomalyDetail().contains("30000원"));
	}

	private CategoryMappingRow row(
			Integer ruleId,
			String keyword,
			Integer categoryId,
			String categoryName,
			Integer threshold,
			String gifUrl) {
		return new CategoryMappingRow() {
			@Override
			public Integer getRuleId() {
				return ruleId;
			}

			@Override
			public String getKeyword() {
				return keyword;
			}

			@Override
			public Integer getCategoryId() {
				return categoryId;
			}

			@Override
			public String getCategoryName() {
				return categoryName;
			}

			@Override
			public Integer getDutchThreshold() {
				return threshold;
			}

			@Override
			public String getGifUrl() {
				return gifUrl;
			}
		};
	}
}
