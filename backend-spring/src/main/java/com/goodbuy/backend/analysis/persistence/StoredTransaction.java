package com.goodbuy.backend.analysis.persistence;

import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;

import java.util.UUID;

public record StoredTransaction(
		UUID id,
		ClassifiedTransaction transaction) {
}
