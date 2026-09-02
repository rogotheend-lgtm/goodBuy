package com.goodbuy.backend.analysis.api;

import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "사용자가 확인 필요 거래를 최종 분류하는 요청")
public record TransactionReviewRequest(
		@Schema(
				description = "확정할 거래 유형. NEEDS_REVIEW는 사용할 수 없습니다.",
				example = "EXPENSE",
				allowableValues = {"EXPENSE", "SELF_TRANSFER", "OTHER_PERSON"})
		@NotNull TransactionType transactionType,
		@Schema(description = "소비로 확정할 때 사용할 목적 카테고리", example = "FOOD")
		PurposeCategory purposeCategory,
		@Schema(description = "본인 부담액. 소비로 확정할 때 1원 이상 원본 금액 이하", example = "630", minimum = "1")
		Long personalAmount) {
}
