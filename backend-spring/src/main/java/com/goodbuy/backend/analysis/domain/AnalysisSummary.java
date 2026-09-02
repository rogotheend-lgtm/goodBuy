package com.goodbuy.backend.analysis.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "거래 분류 후 계산된 최종 분석 요약")
public record AnalysisSummary(
		@Schema(description = "OCR 파싱 전체 건수", example = "10")
		int parsedCount,
		@Schema(description = "OCR 파싱 원본 총액", example = "57680")
		long parsedAmount,
		@Schema(description = "확정 소비 건수", example = "8")
		int expenseCount,
		@Schema(description = "확정된 본인 소비 금액", example = "56430")
		long expenseAmount,
		@Schema(description = "자가 이체로 제외된 금액", example = "0")
		long selfTransferAmount,
		@Schema(description = "다른 사람 거래로 제외된 금액", example = "0")
		long otherPersonAmount,
		@Schema(description = "사용자 확인이 필요한 건수", example = "2")
		int needsReviewCount) {
}
