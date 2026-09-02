package com.goodbuy.backend.analysis;

import com.goodbuy.backend.analysis.domain.AnalysisSummary;
import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.analysis.domain.MerchantType;
import com.goodbuy.backend.analysis.domain.ReviewReason;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
		assertFalse(transaction.requiresReview());
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
	void requiresReviewForAmbiguousPaymentGateway() {
		ClassifiedTransaction transaction = classifier.classify(
				"김세빈",
				new OcrTransactionItem("토스페이_TOSS", 630));

		assertEquals(TransactionType.NEEDS_REVIEW, transaction.transactionType());
		assertEquals(ReviewReason.AMBIGUOUS_PAYMENT_GATEWAY, transaction.reviewReason());
		assertTrue(transaction.requiresReview());
	}

	@Test
	void detectsLargeCafePaymentButNotNormalMeatRestaurantPayment() {
		ClassifiedTransaction cafe = classifier.classify(
				"김세빈",
				new OcrTransactionItem("벌크커피", 30_000));
		ClassifiedTransaction meatRestaurant = classifier.classify(
				"김세빈",
				new OcrTransactionItem("소촌숯불갈비", 40_000));

		assertEquals(TransactionType.NEEDS_REVIEW, cafe.transactionType());
		assertEquals(ReviewReason.GROUP_PAYMENT_CANDIDATE, cafe.reviewReason());
		assertEquals(TransactionType.EXPENSE, meatRestaurant.transactionType());
	}

	@Test
	void calculatesOnlyConfirmedPersonalExpenses() {
		List<ClassifiedTransaction> transactions = List.of(
				classifier.classify("김세빈", new OcrTransactionItem("김세빈", 50_000)),
				classifier.classify("김세빈", new OcrTransactionItem("맘스터치", 7_900)),
				classifier.classify("김세빈", new OcrTransactionItem("토스페이_TOSS", 630)));

		AnalysisSummary summary = new AnalysisSummaryCalculator().calculate(transactions);

		assertEquals(58_530, summary.parsedAmount());
		assertEquals(7_900, summary.expenseAmount());
		assertEquals(50_000, summary.selfTransferAmount());
		assertEquals(1, summary.needsReviewCount());
	}
}
