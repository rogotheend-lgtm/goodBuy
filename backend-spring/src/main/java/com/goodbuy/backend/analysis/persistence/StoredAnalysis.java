package com.goodbuy.backend.analysis.persistence;

import com.goodbuy.backend.analysis.domain.AnalysisStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StoredAnalysis(
		UUID id,
		AnalysisStatus status,
		String failureReason,
		Instant createdAt,
		Instant completedAt,
		List<StoredTransaction> transactions) {
}
