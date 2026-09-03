package com.goodbuy.backend.analysis.domain;

public record MerchantClassification(
		MerchantType merchantType,
		PurposeCategory purposeCategory,
		int dutchThreshold,
		String gifUrl) {
}
