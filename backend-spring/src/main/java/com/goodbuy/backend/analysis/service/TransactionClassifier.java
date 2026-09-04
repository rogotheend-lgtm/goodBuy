package com.goodbuy.backend.analysis.service;

import com.goodbuy.backend.analysis.domain.AnomalyReason;
import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.analysis.domain.MerchantClassification;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.domain.TransactionType;
import com.goodbuy.backend.catalog.CategoryMapping;
import com.goodbuy.backend.ocr.dto.OcrTransactionItem;
import org.springframework.stereotype.Component;

import java.util.List;

/** OCR 거래 한 건에 자기이체, DB 카테고리, 이상치 규칙을 순서대로 적용합니다. */
@Component
public class TransactionClassifier {

	private final NameNormalizer nameNormalizer;
	private final MerchantClassifier merchantClassifier;

	public TransactionClassifier(NameNormalizer nameNormalizer, MerchantClassifier merchantClassifier) {
		this.nameNormalizer = nameNormalizer;
		this.merchantClassifier = merchantClassifier;
	}

	public ClassifiedTransaction classify(
			String ownerName,
			OcrTransactionItem item,
			List<CategoryMapping> categoryMappings) {
		// 1순위: 거래 상대가 입력한 본인 이름이면 소비에서 제외하고 이유를 출력합니다.
		if (isSelfTransfer(ownerName, item.counterparty())) {
			return new ClassifiedTransaction(
					item.counterparty(),
					item.amount(),
					0,
					TransactionType.SELF_TRANSFER,
					PurposeCategory.OTHER,
					null,
					true,
					AnomalyReason.SELF_TRANSFER,
					"입력한 이름과 거래 상대명이 일치하여 본인 계좌 이체로 판단했습니다. 소비 합계에서 제외했습니다.");
		}

		MerchantClassification merchant = merchantClassifier.classify(item.counterparty(), categoryMappings);

		// 2순위: 결제와 송금을 구분할 수 없는 간편결제명은 이상치로 알리고 합계에서 제외합니다.
		if (isAmbiguousPaymentGateway(item.counterparty())) {
			return excludedAnomaly(
					item,
					merchant,
					AnomalyReason.AMBIGUOUS_PAYMENT_GATEWAY,
					"결제 플랫폼명만으로 결제와 송금을 구분할 수 없어 이상치로 표시했습니다. 소비 합계에서 제외했습니다.");
		}

		// 3순위: DB 카테고리 기준을 초과한 거래는 합계에 포함하고 경고만 출력합니다.
		if (item.amount() > merchant.dutchThreshold()) {
			return new ClassifiedTransaction(
					item.counterparty(),
					item.amount(),
					item.amount(),
					TransactionType.EXPENSE,
					merchant.purposeCategory(),
					merchant.gifUrl(),
					true,
					AnomalyReason.GROUP_PAYMENT_CANDIDATE,
					"카테고리 " + merchant.purposeCategory() + "의 기준 금액 " + merchant.dutchThreshold()
							+ "원을 초과한 거래입니다. 단체 결제 가능성을 표시했으며 소비 합계에는 포함했습니다.");
		}

		return new ClassifiedTransaction(
				item.counterparty(),
				item.amount(),
				item.amount(),
				TransactionType.EXPENSE,
				merchant.purposeCategory(),
				merchant.gifUrl(),
				false,
				AnomalyReason.NONE,
				null);
	}

	private boolean isSelfTransfer(String ownerName, String counterparty) {
		String normalizedOwnerName = nameNormalizer.normalize(ownerName);
		return !normalizedOwnerName.isBlank()
				&& normalizedOwnerName.equals(nameNormalizer.normalize(counterparty));
	}

	private boolean isAmbiguousPaymentGateway(String counterparty) {
		String normalized = nameNormalizer.normalize(counterparty);

		// 소비 카테고리와 별개인 이상치 조건입니다. 기존 키워드 판단을 유지합니다.
		return normalized.contains("토스페이")
				|| normalized.contains("카카오페이")
				|| normalized.contains("네이버페이");
	}

	private ClassifiedTransaction excludedAnomaly(
			OcrTransactionItem item,
			MerchantClassification merchant,
			AnomalyReason anomalyReason,
			String anomalyDetail) {
		return new ClassifiedTransaction(
				item.counterparty(),
				item.amount(),
				0,
				TransactionType.ANOMALY,
				merchant.purposeCategory(),
				merchant.gifUrl(),
				true,
				anomalyReason,
				anomalyDetail);
	}
}
