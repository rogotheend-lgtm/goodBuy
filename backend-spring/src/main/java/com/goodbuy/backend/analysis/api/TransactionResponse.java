package com.goodbuy.backend.analysis.api;

import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.domain.AnomalyReason;
import com.goodbuy.backend.analysis.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "분류된 거래")
public record TransactionResponse(
		@Schema(description = "가맹점, 이체 수신자 또는 결제 플랫폼", example = "토스페이_TOSS")
		String counterparty,
		@Schema(description = "OCR에서 인식한 원본 금액", example = "630")
		long originalAmount,
		@Schema(description = "소비 합계에 포함할 본인 부담액", example = "0")
		long personalAmount,
		@Schema(description = "거래 분류", example = "ANOMALY")
		TransactionType transactionType,
		@Schema(description = "소비 목적 카테고리", example = "OTHER")
		PurposeCategory purposeCategory,
		@Schema(description = "시스템이 감지한 이상치 여부", example = "true")
		boolean anomaly,
		@Schema(description = "이상치 감지 이유", example = "AMBIGUOUS_PAYMENT_GATEWAY")
		AnomalyReason anomalyReason,
		@Schema(
				description = "사용자에게 출력할 이상치 상세 설명. 정상 거래는 null",
				example = "결제 플랫폼명만으로 결제와 송금을 구분할 수 없어 이상치로 표시했습니다. 소비 합계에서 제외했습니다.",
				nullable = true)
		String anomalyDetail) {

	public static TransactionResponse from(ClassifiedTransaction transaction) {
		return new TransactionResponse(
				transaction.counterparty(),
				transaction.originalAmount(),
				transaction.personalAmount(),
				transaction.transactionType(),
				transaction.purposeCategory(),
				transaction.anomaly(),
				transaction.anomalyReason(),
				transaction.anomalyDetail());
	}
}
