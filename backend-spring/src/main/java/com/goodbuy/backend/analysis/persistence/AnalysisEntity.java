package com.goodbuy.backend.analysis.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** 외부에 노출하지 않는 분석 저장 단위입니다. */
@Entity
@Table(name = "analysis")
class AnalysisEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 20)
	private String status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	protected AnalysisEntity() {
	}

	AnalysisEntity(UUID id, Instant completedAt) {
		this.id = id;
		this.status = "COMPLETED";
		this.createdAt = completedAt;
		this.completedAt = completedAt;
	}
}
