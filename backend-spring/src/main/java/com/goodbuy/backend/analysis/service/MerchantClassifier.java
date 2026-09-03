package com.goodbuy.backend.analysis.service;

import com.goodbuy.backend.analysis.domain.MerchantClassification;
import com.goodbuy.backend.analysis.domain.MerchantType;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import org.springframework.stereotype.Component;

import java.util.List;

/** 거래 상대 이름의 키워드로 가맹점 형태와 소비 목적 카테고리를 찾습니다. */
@Component
public class MerchantClassifier {

	private static final List<MerchantRule> RULES = List.of(
			new MerchantRule(List.of("토스페이", "카카오페이", "네이버페이"), MerchantType.PAYMENT_GATEWAY, PurposeCategory.OTHER),
			new MerchantRule(List.of("삼겹", "갈비", "고기", "정육", "숯불"), MerchantType.MEAT_RESTAURANT, PurposeCategory.FOOD),
			new MerchantRule(List.of("커피", "카페", "스타벅스", "투썸"), MerchantType.CAFE, PurposeCategory.FOOD),
			new MerchantRule(List.of("맘스터치", "버거킹", "맥도날드", "롯데리아"), MerchantType.FAST_FOOD, PurposeCategory.FOOD),
			new MerchantRule(List.of("세븐일레븐", "GS25", "이마트24"), MerchantType.CONVENIENCE_STORE, PurposeCategory.LIVING),
			new MerchantRule(List.of("다이소"), MerchantType.HOUSEHOLD_STORE, PurposeCategory.LIVING),
			new MerchantRule(List.of("마트"), MerchantType.MART, PurposeCategory.LIVING),
			new MerchantRule(List.of("칼국수", "식당", "버거앤타코"), MerchantType.RESTAURANT, PurposeCategory.FOOD));

	private final NameNormalizer normalizer;

	public MerchantClassifier(NameNormalizer normalizer) {
		this.normalizer = normalizer;
	}

	public MerchantClassification classify(String counterparty) {
		String normalizedCounterparty = normalizer.normalize(counterparty);

		// 먼저 일치한 규칙을 사용하고, 알 수 없는 상호는 OTHER로 남깁니다.
		return RULES.stream()
				.filter(rule -> rule.keywords().stream().anyMatch(normalizedCounterparty::contains))
				.findFirst()
				.map(rule -> new MerchantClassification(rule.merchantType(), rule.purposeCategory()))
				.orElse(new MerchantClassification(MerchantType.OTHER, PurposeCategory.OTHER));
	}

	private record MerchantRule(
			List<String> keywords,
			MerchantType merchantType,
			PurposeCategory purposeCategory) {
	}
}
