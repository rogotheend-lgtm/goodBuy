package com.goodbuy.backend.analysis.service;

import com.goodbuy.backend.analysis.domain.MerchantClassification;
import com.goodbuy.backend.analysis.domain.MerchantType;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.catalog.CategoryMapping;
import org.springframework.stereotype.Component;

import java.util.List;

/** DB 카테고리 키워드로 목적 카테고리를 찾고 기존 응답용 가맹점 형태를 판별합니다. */
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
				resolveMerchantType(normalizedCounterparty),
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

	/** merchantType은 기존 응답 호환과 결제 플랫폼 판별에만 사용합니다. 카테고리는 DB가 결정합니다. */
	private MerchantType resolveMerchantType(String counterparty) {
		if (containsAny(counterparty, "토스페이", "카카오페이", "네이버페이")) {
			return MerchantType.PAYMENT_GATEWAY;
		}
		if (containsAny(counterparty, "삼겹", "갈비", "고기", "정육", "숯불")) {
			return MerchantType.MEAT_RESTAURANT;
		}
		if (containsAny(counterparty, "커피", "카페", "스타벅스", "투썸")) {
			return MerchantType.CAFE;
		}
		if (containsAny(counterparty, "맘스터치", "버거킹", "맥도날드", "롯데리아")) {
			return MerchantType.FAST_FOOD;
		}
		if (containsAny(counterparty, "세븐일레븐", "GS25", "이마트24")) {
			return MerchantType.CONVENIENCE_STORE;
		}
		if (containsAny(counterparty, "다이소")) {
			return MerchantType.HOUSEHOLD_STORE;
		}
		if (containsAny(counterparty, "마트")) {
			return MerchantType.MART;
		}
		if (containsAny(counterparty, "칼국수", "식당", "버거앤타코")) {
			return MerchantType.RESTAURANT;
		}
		return MerchantType.OTHER;
	}

	private boolean containsAny(String value, String... keywords) {
		return List.of(keywords).stream().anyMatch(value::contains);
	}
}
