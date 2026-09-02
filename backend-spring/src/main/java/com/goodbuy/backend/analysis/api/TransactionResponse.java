package com.goodbuy.backend.analysis.api;

import com.goodbuy.backend.analysis.domain.DecisionSource;
import com.goodbuy.backend.analysis.domain.MerchantType;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.domain.ReviewReason;
import com.goodbuy.backend.analysis.domain.TransactionType;
import com.goodbuy.backend.analysis.persistence.StoredTransaction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "분류된 거래")
public record TransactionResponse(
		@Schema(description = "거래 ID")
		UUID id,
		@Schema(description = "가맹점, 이체 수신자 또는 결제 플랫폼", example = "토스페이_TOSS")
		String counterparty,
		@Schema(description = "OCR에서 인식한 원본 금액", example = "630")
		long originalAmount,
		@Schema(description = "소비 합계에 포함할 본인 부담액", example = "0")
		long personalAmount,
		@Schema(description = "거래 분류", example = "NEEDS_REVIEW")
		TransactionType transactionType,
		@Schema(description = "소비 목적 카테고리", example = "OTHER")
		PurposeCategory purposeCategory,
		@Schema(description = "가맹점 업종", example = "PAYMENT_GATEWAY")
		MerchantType merchantType,
		@Schema(description = "분류 결정 주체", example = "SYSTEM")
		DecisionSource decisionSource,
		@Schema(description = "사용자 확인 필요 여부", example = "true")
		boolean requiresReview,
		@Schema(description = "사용자 확인이 필요한 이유", example = "AMBIGUOUS_PAYMENT_GATEWAY")
		ReviewReason reviewReason) {

	public static TransactionResponse from(StoredTransaction storedTransaction) {
		var transaction = storedTransaction.transaction();
		return new TransactionResponse(
				storedTransaction.id(),
				transaction.counterparty(),
				transaction.originalAmount(),
				transaction.personalAmount(),
				transaction.transactionType(),
				transaction.purposeCategory(),
				transaction.merchantType(),
				transaction.decisionSource(),
				transaction.requiresReview(),
				transaction.reviewReason());
	}
}
