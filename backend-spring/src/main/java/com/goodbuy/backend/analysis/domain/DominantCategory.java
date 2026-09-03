package com.goodbuy.backend.analysis.domain;

/** 확정 소비 중 가장 큰 금액을 차지한 카테고리 정보입니다. */
public record DominantCategory(
		PurposeCategory purposeCategory,
		long amount,
		int ratioPercent,
		String gifUrl) {
}
