package com.goodbuy.backend.analysis.api;

import com.goodbuy.backend.analysis.domain.AnalysisSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "소비 분석 결과")
public record AnalysisResponse(
		@Schema(description = "분류된 거래 목록")
		List<TransactionResponse> transactions,
		@Schema(description = "Spring이 다시 계산한 최종 요약")
		AnalysisSummary summary) {
}
