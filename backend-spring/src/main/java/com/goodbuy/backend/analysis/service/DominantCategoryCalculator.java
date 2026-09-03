package com.goodbuy.backend.analysis.service;

import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.analysis.domain.DominantCategory;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.domain.TransactionType;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 확정 소비를 카테고리별로 합산해 가장 큰 비중의 카테고리를 찾습니다. */
@Component
public class DominantCategoryCalculator {

	public Optional<DominantCategory> calculate(
			List<ClassifiedTransaction> transactions,
			long expenseAmount) {
		if (expenseAmount <= 0) {
			return Optional.empty();
		}

		Map<PurposeCategory, CategoryTotal> totals = new EnumMap<>(PurposeCategory.class);
		transactions.stream()
				.filter(transaction -> transaction.transactionType() == TransactionType.EXPENSE)
				.forEach(transaction -> totals.compute(
						transaction.purposeCategory(),
						(category, current) -> current == null
								? new CategoryTotal(transaction.personalAmount(), transaction.categoryGifUrl())
								: current.add(transaction.personalAmount(), transaction.categoryGifUrl())));

		return totals.entrySet().stream()
				.max(Comparator
						.<Map.Entry<PurposeCategory, CategoryTotal>>comparingLong(entry -> entry.getValue().amount())
						.thenComparing(entry -> entry.getKey().name(), Comparator.reverseOrder()))
				.map(entry -> new DominantCategory(
						entry.getKey(),
						entry.getValue().amount(),
						(int) Math.round(entry.getValue().amount() * 100.0 / expenseAmount),
						entry.getValue().gifUrl()));
	}

	private record CategoryTotal(long amount, String gifUrl) {

		private CategoryTotal add(long additionalAmount, String additionalGifUrl) {
			String resolvedGifUrl = gifUrl == null || gifUrl.isBlank() ? additionalGifUrl : gifUrl;
			return new CategoryTotal(Math.addExact(amount, additionalAmount), resolvedGifUrl);
		}
	}
}
