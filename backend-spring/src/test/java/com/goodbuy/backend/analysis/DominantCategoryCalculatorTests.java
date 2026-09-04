package com.goodbuy.backend.analysis;

import com.goodbuy.backend.analysis.domain.AnomalyReason;
import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.domain.TransactionType;
import com.goodbuy.backend.analysis.service.DominantCategoryCalculator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DominantCategoryCalculatorTests {

	private final DominantCategoryCalculator calculator = new DominantCategoryCalculator();

	@Test
	void calculatesDominantExpenseCategoryAndItsGif() {
		var result = calculator.calculate(
				List.of(
						expense(PurposeCategory.FOOD, 20_000, "food.gif"),
						expense(PurposeCategory.FOOD, 10_000, "food.gif"),
						expense(PurposeCategory.LIVING, 20_000, "living.gif")),
				50_000).orElseThrow();

		assertEquals(PurposeCategory.FOOD, result.purposeCategory());
		assertEquals(30_000, result.amount());
		assertEquals(60, result.ratioPercent());
		assertEquals("food.gif", result.gifUrl());
	}

	@Test
	void returnsEmptyWhenThereIsNoConfirmedExpense() {
		assertTrue(calculator.calculate(List.of(), 0).isEmpty());
	}

	private ClassifiedTransaction expense(PurposeCategory category, long amount, String gifUrl) {
		return new ClassifiedTransaction(
				"거래처",
				amount,
				amount,
				TransactionType.EXPENSE,
				category,
				gifUrl,
				false,
				AnomalyReason.NONE,
				null);
	}
}
