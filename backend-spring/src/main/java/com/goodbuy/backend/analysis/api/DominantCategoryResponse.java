package com.goodbuy.backend.analysis.api;

import com.goodbuy.backend.analysis.domain.DominantCategory;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "확정 소비에서 가장 큰 비중을 차지한 카테고리")
public record DominantCategoryResponse(
		@Schema(description = "소비 목적 카테고리", example = "FOOD")
		PurposeCategory purposeCategory,
		@Schema(description = "해당 카테고리의 확정 소비 금액", example = "30000")
		long amount,
		@Schema(description = "전체 확정 소비 중 비율(반올림한 정수)", example = "53")
		int ratioPercent,
		@Schema(description = "카테고리에 연결된 GIF URL")
		String gifUrl) {

	public static DominantCategoryResponse from(DominantCategory category) {
		if (category == null) {
			return null;
		}
		return new DominantCategoryResponse(
				category.purposeCategory(),
				category.amount(),
				category.ratioPercent(),
				category.gifUrl());
	}
}
