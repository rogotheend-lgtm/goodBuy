package com.goodbuy.backend.analysis.service;

import com.goodbuy.backend.analysis.domain.MerchantClassification;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.catalog.CategoryMapping;
import org.springframework.stereotype.Component;

import java.util.List;

/** DB/Fallback 키워드로 정식 카테고리와 해당 기준금액·GIF를 선택합니다. */
@Component
public class MerchantClassifier {

	private final NameNormalizer normalizer;

	public MerchantClassifier(NameNormalizer normalizer) {
		this.normalizer = normalizer;
	}

	public MerchantClassification classify(String counterparty, List<CategoryMapping> mappings) {
		String normalizedCounterparty = normalizer.normalize(counterparty);
		CategoryMapping category = mappings.stream()
				.filter(mapping -> mapping.normalizedKeyword() != null && !mapping.normalizedKeyword().isBlank())
				.filter(mapping -> normalizedCounterparty.contains(mapping.normalizedKeyword()))
				.findFirst()
				.orElseGet(() -> findOtherCategory(mappings));

		return new MerchantClassification(
				category.purposeCategory(),
				category.dutchThreshold(),
				category.gifUrl());
	}

	private CategoryMapping findOtherCategory(List<CategoryMapping> mappings) {
		return mappings.stream()
				.filter(mapping -> mapping.purposeCategory() == PurposeCategory.OTHER)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Category map must contain the OTHER category"));
	}

}
