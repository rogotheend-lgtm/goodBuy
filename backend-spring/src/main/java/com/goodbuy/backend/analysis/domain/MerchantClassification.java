package com.goodbuy.backend.analysis.domain;

/** DB 또는 기본 카탈로그에서 선택한 정식 카테고리와 부가 정보입니다. */
public record MerchantClassification(
		PurposeCategory purposeCategory,
		int dutchThreshold,
		String gifUrl) {
}
