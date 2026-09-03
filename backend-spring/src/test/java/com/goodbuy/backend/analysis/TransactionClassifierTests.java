package com.goodbuy.backend.analysis;

import com.goodbuy.backend.analysis.domain.AnalysisSummary;
import com.goodbuy.backend.analysis.domain.AnomalyReason;
import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.analysis.domain.MerchantType;
import com.goodbuy.backend.analysis.domain.TransactionType;
import com.goodbuy.backend.analysis.service.AnalysisSummaryCalculator;
import com.goodbuy.backend.analysis.service.GroupPaymentPolicy;
import com.goodbuy.backend.analysis.service.MerchantClassifier;
import com.goodbuy.backend.analysis.service.NameNormalizer;
import com.goodbuy.backend.analysis.service.TransactionClassifier;
import com.goodbuy.backend.ocr.dto.OcrTransactionItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionClassifierTests {

	private TransactionClassifier classifier;

	@BeforeEach
	void setUp() {
		NameNormalizer normalizer = new NameNormalizer();
		classifier = new TransactionClassifier(
				normalizer,
				new MerchantClassifier(normalizer),
				new GroupPaymentPolicy());
	}

	@Test
	void excludesExactNormalizedOwnerNameAsSelfTransfer() {
		ClassifiedTransaction transaction = classifier.classify(
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
		ClassifiedTransaction transaction = classifier.classify(
				"김세빈",
				new OcrTransactionItem("김세빈커피", 3_000));

		assertEquals(TransactionType.EXPENSE, transaction.transactionType());
		assertEquals(MerchantType.CAFE, transaction.merchantType());
	}

	@Test
	void outputsAnomalyForAmbiguousPaymentGateway() {
		ClassifiedTransaction transaction = classifier.classify(
				"김세빈",
				new OcrTransactionItem("토스페이_TOSS", 630));

		assertEquals(TransactionType.ANOMALY, transaction.transactionType());
		assertEquals(AnomalyReason.AMBIGUOUS_PAYMENT_GATEWAY, transaction.anomalyReason());
		assertTrue(transaction.anomaly());
		assertTrue(transaction.anomalyDetail().contains("결제와 송금을 구분"));
	}

	@Test
	void detectsLargeCafePaymentButNotNormalMeatRestaurantPayment() {
		ClassifiedTransaction cafe = classifier.classify(
				"김세빈",
				new OcrTransactionItem("벌크커피", 30_000));
		ClassifiedTransaction meatRestaurant = classifier.classify(
				"김세빈",
				new OcrTransactionItem("소촌숯불갈비", 40_000));

		assertEquals(TransactionType.ANOMALY, cafe.transactionType());
		assertEquals(AnomalyReason.GROUP_PAYMENT_CANDIDATE, cafe.anomalyReason());
		assertTrue(cafe.anomalyDetail().contains("3배 기준"));
		assertEquals(TransactionType.EXPENSE, meatRestaurant.transactionType());
	}

	@Test
	void calculatesExpensesAndOutputsAllDetectedAnomalies() {
		List<ClassifiedTransaction> transactions = List.of(
				classifier.classify("김세빈", new OcrTransactionItem("김세빈", 50_000)),
				classifier.classify("김세빈", new OcrTransactionItem("맘스터치", 7_900)),
				classifier.classify("김세빈", new OcrTransactionItem("토스페이_TOSS", 630)));

		AnalysisSummary summary = new AnalysisSummaryCalculator().calculate(transactions);

		assertEquals(58_530, summary.parsedAmount());
		assertEquals(7_900, summary.expenseAmount());
		assertEquals(50_000, summary.selfTransferAmount());
		assertEquals(2, summary.anomalyCount());
		assertEquals(50_630, summary.anomalyAmount());
	}
}
