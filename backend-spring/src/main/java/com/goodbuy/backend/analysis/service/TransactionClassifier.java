package com.goodbuy.backend.analysis.service;

import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.analysis.domain.DecisionSource;
import com.goodbuy.backend.analysis.domain.MerchantClassification;
import com.goodbuy.backend.analysis.domain.MerchantType;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.domain.ReviewReason;
import com.goodbuy.backend.analysis.domain.TransactionType;
import com.goodbuy.backend.ocr.dto.OcrTransactionItem;
import org.springframework.stereotype.Component;

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
		if (isSelfTransfer(ownerName, item.counterparty())) {
			return new ClassifiedTransaction(
					item.counterparty(),
					item.amount(),
					0,
					TransactionType.SELF_TRANSFER,
					PurposeCategory.OTHER,
					MerchantType.OTHER,
					DecisionSource.SYSTEM,
					false,
					ReviewReason.NONE);
		}

		MerchantClassification merchant = merchantClassifier.classify(item.counterparty());
		if (merchant.merchantType() == MerchantType.PAYMENT_GATEWAY) {
			return needsReview(item, merchant, ReviewReason.AMBIGUOUS_PAYMENT_GATEWAY);
		}

		if (groupPaymentPolicy.isCandidate(merchant.merchantType(), item.amount())) {
			return needsReview(item, merchant, ReviewReason.GROUP_PAYMENT_CANDIDATE);
		}

		return new ClassifiedTransaction(
				item.counterparty(),
				item.amount(),
				item.amount(),
				TransactionType.EXPENSE,
				merchant.purposeCategory(),
				merchant.merchantType(),
				DecisionSource.SYSTEM,
				false,
				ReviewReason.NONE);
	}

	private boolean isSelfTransfer(String ownerName, String counterparty) {
		String normalizedOwnerName = nameNormalizer.normalize(ownerName);
		return !normalizedOwnerName.isBlank()
				&& normalizedOwnerName.equals(nameNormalizer.normalize(counterparty));
	}

	private ClassifiedTransaction needsReview(
			OcrTransactionItem item,
			MerchantClassification merchant,
			ReviewReason reviewReason) {
		return new ClassifiedTransaction(
				item.counterparty(),
				item.amount(),
				0,
				TransactionType.NEEDS_REVIEW,
				merchant.purposeCategory(),
				merchant.merchantType(),
				DecisionSource.SYSTEM,
				true,
				reviewReason);
	}
}
