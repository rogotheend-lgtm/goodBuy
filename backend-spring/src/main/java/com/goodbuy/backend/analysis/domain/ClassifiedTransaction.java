package com.goodbuy.backend.analysis.domain;

public record ClassifiedTransaction(
		String counterparty,
		long originalAmount,
		long personalAmount,
		TransactionType transactionType,
		PurposeCategory purposeCategory,
		String categoryGifUrl,
		MerchantType merchantType,
		boolean anomaly,
		AnomalyReason anomalyReason,
		String anomalyDetail) {
}
