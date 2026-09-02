package com.goodbuy.backend.analysis.service;

import com.goodbuy.backend.analysis.domain.AnalysisSummary;
import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.analysis.domain.TransactionType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnalysisSummaryCalculator {

	public AnalysisSummary calculate(List<ClassifiedTransaction> transactions) {
		long parsedAmount = sum(transactions, AmountType.ORIGINAL);
		long expenseAmount = sum(
				transactions.stream().filter(transaction -> transaction.transactionType() == TransactionType.EXPENSE).toList(),
				AmountType.PERSONAL);
		long selfTransferAmount = sum(
				transactions.stream().filter(transaction -> transaction.transactionType() == TransactionType.SELF_TRANSFER).toList(),
				AmountType.ORIGINAL);
		long otherPersonAmount = sum(
				transactions.stream().filter(transaction -> transaction.transactionType() == TransactionType.OTHER_PERSON).toList(),
				AmountType.ORIGINAL);

		return new AnalysisSummary(
				transactions.size(),
				parsedAmount,
				(int) transactions.stream().filter(transaction -> transaction.transactionType() == TransactionType.EXPENSE).count(),
				expenseAmount,
				selfTransferAmount,
				otherPersonAmount,
				(int) transactions.stream().filter(ClassifiedTransaction::requiresReview).count());
	}

	private long sum(List<ClassifiedTransaction> transactions, AmountType amountType) {
		try {
			return transactions.stream()
					.mapToLong(transaction -> amountType == AmountType.ORIGINAL
							? transaction.originalAmount()
							: transaction.personalAmount())
					.reduce(0L, Math::addExact);
		} catch (ArithmeticException exception) {
			throw new IllegalArgumentException("Transaction amount sum overflowed", exception);
		}
	}

	private enum AmountType {
		ORIGINAL,
		PERSONAL
	}
}
