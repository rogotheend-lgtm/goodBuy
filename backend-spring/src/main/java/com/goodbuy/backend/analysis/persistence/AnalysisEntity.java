package com.goodbuy.backend.analysis.persistence;

import com.goodbuy.backend.analysis.domain.AnalysisStatus;
import com.goodbuy.backend.session.AnonymousSessionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis")
public class AnalysisEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "anonymous_session_id", nullable = false)
	private AnonymousSessionEntity anonymousSession;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AnalysisStatus status;

	@Column(name = "failure_reason", length = 500)
	private String failureReason;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	protected AnalysisEntity() {
	}

	public AnalysisEntity(UUID id, AnonymousSessionEntity anonymousSession, Instant createdAt) {
		this.id = id;
		this.anonymousSession = anonymousSession;
		this.status = AnalysisStatus.PENDING;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public AnalysisStatus getStatus() {
		return status;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public void markProcessing() {
		this.status = AnalysisStatus.PROCESSING;
	}

	public void markCompleted(Instant completedAt) {
		this.status = AnalysisStatus.COMPLETED;
		this.completedAt = completedAt;
		this.failureReason = null;
	}

	public void markFailed(String failureReason, Instant completedAt) {
		this.status = AnalysisStatus.FAILED;
		this.failureReason = failureReason;
		this.completedAt = completedAt;
	}
}
