package com.goodbuy.backend.analysis.api;

import com.goodbuy.backend.analysis.domain.AnalysisSummary;
import com.goodbuy.backend.catalog.CategoryCatalogSource;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "소비 분석 결과")
public record AnalysisResponse(
		@Schema(description = "분류된 거래 목록")
		List<TransactionResponse> transactions,
		@Schema(description = "Spring이 다시 계산한 최종 요약")
		AnalysisSummary summary,
		@Schema(description = "카테고리 기준 출처", example = "DATABASE")
		CategoryCatalogSource categoryCatalogSource,
		@Schema(description = "확정 소비 중 가장 큰 비중의 카테고리. 확정 소비가 없으면 null", nullable = true)
		DominantCategoryResponse dominantCategory) {
}
