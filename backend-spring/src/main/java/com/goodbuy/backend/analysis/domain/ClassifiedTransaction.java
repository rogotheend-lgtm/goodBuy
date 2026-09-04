package com.goodbuy.backend.analysis.domain;

public record ClassifiedTransaction(
		String counterparty,
		long originalAmount,
		long personalAmount,
		TransactionType transactionType,
		PurposeCategory purposeCategory,
		String categoryGifUrl,
		boolean anomaly,
		AnomalyReason anomalyReason,
		String anomalyDetail) {
}
