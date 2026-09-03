package com.goodbuy.backend.catalog;

import com.goodbuy.backend.analysis.domain.PurposeCategory;

/** Spring이 한 번의 분석 동안 사용할 정규화된 카테고리 규칙입니다. */
public record CategoryMapping(
		Integer ruleId,
		Integer categoryId,
		String normalizedKeyword,
		PurposeCategory purposeCategory,
		int dutchThreshold,
		String gifUrl) {
}
