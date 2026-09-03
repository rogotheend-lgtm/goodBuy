package com.goodbuy.backend.analysis;

import com.goodbuy.backend.analysis.domain.AnalysisSummary;
import com.goodbuy.backend.analysis.domain.AnomalyReason;
import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.analysis.domain.MerchantType;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.domain.TransactionType;
import com.goodbuy.backend.analysis.service.AnalysisSummaryCalculator;
import com.goodbuy.backend.analysis.service.MerchantClassifier;
import com.goodbuy.backend.analysis.service.NameNormalizer;
import com.goodbuy.backend.analysis.service.TransactionClassifier;
import com.goodbuy.backend.catalog.CategoryMapping;
import com.goodbuy.backend.ocr.dto.OcrTransactionItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionClassifierTests {

	private TransactionClassifier classifier;
	private List<CategoryMapping> categoryMappings;

	@BeforeEach
	void setUp() {
		NameNormalizer normalizer = new NameNormalizer();
		classifier = new TransactionClassifier(
				normalizer,
				new MerchantClassifier(normalizer));
		categoryMappings = List.of(
				mapping(1, 1, "버거앤타코", PurposeCategory.FOOD, 30_000),
				mapping(2, 1, "맘스터치", PurposeCategory.FOOD, 30_000),
				mapping(3, 1, "벌크커피", PurposeCategory.FOOD, 30_000),
				mapping(4, 1, "커피", PurposeCategory.FOOD, 30_000),
				mapping(5, 1, "갈비", PurposeCategory.FOOD, 30_000),
				mapping(null, 9, null, PurposeCategory.OTHER, 100_000));
	}

	@Test
	void excludesExactNormalizedOwnerNameAsSelfTransfer() {
		ClassifiedTransaction transaction = classify(
				"김 세 빈",
				new OcrTransactionItem("김세빈", 50_000));

		assertEquals(TransactionType.SELF_TRANSFER, transaction.transactionType());
		assertEquals(0, transaction.personalAmount());
		assertTrue(transaction.anomaly());
		assertEquals(AnomalyReason.SELF_TRANSFER, transaction.anomalyReason());
		assertTrue(transaction.anomalyDetail().contains("본인 계좌 이체"));
	}

	@Test
	void doesNotExcludePartialOwnerNameMatch() {
		ClassifiedTransaction transaction = classify(
				"김세빈",
				new OcrTransactionItem("김세빈커피", 3_000));

		assertEquals(TransactionType.EXPENSE, transaction.transactionType());
		assertEquals(MerchantType.CAFE, transaction.merchantType());
	}

	@Test
	void outputsAnomalyForAmbiguousPaymentGateway() {
		ClassifiedTransaction transaction = classify(
				"김세빈",
				new OcrTransactionItem("토스페이_TOSS", 630));

		assertEquals(TransactionType.ANOMALY, transaction.transactionType());
		assertEquals(AnomalyReason.AMBIGUOUS_PAYMENT_GATEWAY, transaction.anomalyReason());
		assertTrue(transaction.anomaly());
		assertTrue(transaction.anomalyDetail().contains("결제와 송금을 구분"));
	}

	@Test
	void includesLargePaymentInExpenseAndOnlyWarnsWhenThresholdIsExceeded() {
		ClassifiedTransaction equalToThreshold = classify(
				"김세빈",
				new OcrTransactionItem("벌크커피", 30_000));
		ClassifiedTransaction overThreshold = classify(
				"김세빈",
				new OcrTransactionItem("소촌숯불갈비", 40_000));

		assertEquals(AnomalyReason.NONE, equalToThreshold.anomalyReason());
		assertEquals(TransactionType.EXPENSE, overThreshold.transactionType());
		assertEquals(40_000, overThreshold.personalAmount());
		assertEquals(AnomalyReason.GROUP_PAYMENT_CANDIDATE, overThreshold.anomalyReason());
		assertTrue(overThreshold.anomalyDetail().contains("소비 합계에는 포함"));
	}

	@Test
	void calculatesExpensesAndOutputsAllDetectedAnomalies() {
		List<ClassifiedTransaction> transactions = List.of(
				classify("김세빈", new OcrTransactionItem("김세빈", 50_000)),
				classify("김세빈", new OcrTransactionItem("맘스터치", 7_900)),
				classify("김세빈", new OcrTransactionItem("소촌숯불갈비", 40_000)),
				classify("김세빈", new OcrTransactionItem("토스페이_TOSS", 630)));

		AnalysisSummary summary = new AnalysisSummaryCalculator().calculate(transactions);

		assertEquals(98_530, summary.parsedAmount());
		assertEquals(47_900, summary.expenseAmount());
		assertEquals(50_000, summary.selfTransferAmount());
		assertEquals(3, summary.anomalyCount());
		assertEquals(90_630, summary.anomalyAmount());
	}

	private ClassifiedTransaction classify(String ownerName, OcrTransactionItem item) {
		return classifier.classify(ownerName, item, categoryMappings);
	}

	private CategoryMapping mapping(
			Integer ruleId,
			Integer categoryId,
			String normalizedKeyword,
			PurposeCategory category,
			int threshold) {
		return new CategoryMapping(ruleId, categoryId, normalizedKeyword, category, threshold, "https://example.com/" + category + ".gif");
	}
}
