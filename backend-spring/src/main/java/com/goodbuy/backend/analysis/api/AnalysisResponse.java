package com.goodbuy.backend.analysis.api;

import com.goodbuy.backend.analysis.domain.AnalysisStatus;
import com.goodbuy.backend.analysis.domain.AnalysisSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "소비 분석 결과")
public record AnalysisResponse(
		@Schema(description = "분석 ID")
		UUID analysisId,
		@Schema(description = "분석 상태", example = "COMPLETED")
		AnalysisStatus status,
		@Schema(description = "실패 원인. 정상 완료 시 null", nullable = true)
		String failureReason,
		@Schema(description = "분석 생성 시각")
		Instant createdAt,
		@Schema(description = "분석 완료 시각", nullable = true)
		Instant completedAt,
		@Schema(description = "분류된 거래 목록")
		List<TransactionResponse> transactions,
		@Schema(description = "Spring이 다시 계산한 최종 요약")
		AnalysisSummary summary) {
}
