package com.goodbuy.backend.analysis.service;

import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.analysis.domain.AnomalyReason;
import com.goodbuy.backend.analysis.domain.MerchantClassification;
import com.goodbuy.backend.analysis.domain.MerchantType;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.domain.TransactionType;
import com.goodbuy.backend.ocr.dto.OcrTransactionItem;
import org.springframework.stereotype.Component;

/**
 * OCR 거래 한 건을 소비, 자가 이체 또는 이상 거래로 분류합니다.
 * 잘못된 소비 합산을 막기 위해 확실한 규칙부터 순서대로 적용합니다.
 */
@Component
public class TransactionClassifier {

	private final NameNormalizer nameNormalizer;
	private final MerchantClassifier merchantClassifier;
	private final GroupPaymentPolicy groupPaymentPolicy;

	public TransactionClassifier(
			NameNormalizer nameNormalizer,
			MerchantClassifier merchantClassifier,
			GroupPaymentPolicy groupPaymentPolicy) {
		this.nameNormalizer = nameNormalizer;
		this.merchantClassifier = merchantClassifier;
		this.groupPaymentPolicy = groupPaymentPolicy;
	}

	public ClassifiedTransaction classify(String ownerName, OcrTransactionItem item) {
		// 1순위: 거래 상대가 입력한 본인 이름이면 소비에서 제외하고 이상치 정보를 출력합니다.
		if (isSelfTransfer(ownerName, item.counterparty())) {
			return new ClassifiedTransaction(
					item.counterparty(),
					item.amount(),
					0,
					TransactionType.SELF_TRANSFER,
					PurposeCategory.OTHER,
					MerchantType.OTHER,
					true,
					AnomalyReason.SELF_TRANSFER,
					"입력한 이름과 거래 상대명이 일치하여 본인 계좌 이체로 판단했습니다. 소비 합계에서 제외했습니다.");
		}

		MerchantClassification merchant = merchantClassifier.classify(item.counterparty());

		// 2순위: 결제와 송금을 구분할 수 없는 간편결제명은 이상치로만 알립니다.
		if (merchant.merchantType() == MerchantType.PAYMENT_GATEWAY) {
			return anomaly(
					item,
					merchant,
					AnomalyReason.AMBIGUOUS_PAYMENT_GATEWAY,
					"결제 플랫폼명만으로 결제와 송금을 구분할 수 없어 이상치로 표시했습니다. 소비 합계에서 제외했습니다.");
		}

		// 3순위: 업종별 1인 기준보다 큰 거래는 단체 결제 가능성을 상세 내용으로 출력합니다.
		if (groupPaymentPolicy.isCandidate(merchant.merchantType(), item.amount())) {
			long referenceAmount = groupPaymentPolicy.referenceAmountPerPerson(merchant.merchantType());
			long threshold = groupPaymentPolicy.candidateThreshold(merchant.merchantType());
			return anomaly(
					item,
					merchant,
					AnomalyReason.GROUP_PAYMENT_CANDIDATE,
					"업종별 1인 예상 금액 " + referenceAmount + "원의 3배 기준(" + threshold
							+ "원) 이상인 거래입니다. 단체 결제 가능성이 있어 소비 합계에서 제외했습니다.");
		}

		// 위 조건에 해당하지 않으면 일반 소비로 확정합니다.
		return new ClassifiedTransaction(
				item.counterparty(),
				item.amount(),
				item.amount(),
				TransactionType.EXPENSE,
				merchant.purposeCategory(),
				merchant.merchantType(),
				false,
				AnomalyReason.NONE,
				null);
	}

	private boolean isSelfTransfer(String ownerName, String counterparty) {
		String normalizedOwnerName = nameNormalizer.normalize(ownerName);
		return !normalizedOwnerName.isBlank()
				&& normalizedOwnerName.equals(nameNormalizer.normalize(counterparty));
	}

	private ClassifiedTransaction anomaly(
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
				merchant.merchantType(),
				true,
				anomalyReason,
				anomalyDetail);
	}
}
