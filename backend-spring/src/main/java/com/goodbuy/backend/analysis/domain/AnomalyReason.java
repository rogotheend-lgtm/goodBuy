package com.goodbuy.backend.analysis.domain;

/** 시스템이 거래를 이상치로 표시한 이유입니다. */
public enum AnomalyReason {
	NONE,
	SELF_TRANSFER,
	AMBIGUOUS_PAYMENT_GATEWAY,
	GROUP_PAYMENT_CANDIDATE
}
