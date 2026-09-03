package com.goodbuy.backend.analysis.service;

import com.goodbuy.backend.analysis.domain.AnalysisSummary;
import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.analysis.domain.TransactionType;
import org.springframework.stereotype.Component;

import java.util.List;

/** 최종 분류된 거래를 기준으로 프론트엔드에 보여줄 요약 금액을 계산합니다. */
@Component
public class AnalysisSummaryCalculator {

	public AnalysisSummary calculate(List<ClassifiedTransaction> transactions) {
		// 원본 총액과 실제 본인 소비액은 목적이 다르므로 각각 계산합니다.
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
		List<ClassifiedTransaction> anomalies = transactions.stream()
				.filter(ClassifiedTransaction::anomaly)
				.toList();

		return new AnalysisSummary(
				transactions.size(),
				parsedAmount,
				(int) transactions.stream().filter(transaction -> transaction.transactionType() == TransactionType.EXPENSE).count(),
				expenseAmount,
				selfTransferAmount,
				otherPersonAmount,
				anomalies.size(),
				sum(anomalies, AmountType.ORIGINAL));
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
