package com.goodbuy.backend.analysis.persistence;

import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
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

/** Spring이 최종 분류한 거래 한 건을 저장합니다. */
@Entity
@Table(name = "expense_transaction")
class ExpenseTransactionEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "analysis_id", nullable = false)
	private AnalysisEntity analysis;

	@Column(name = "item_order", nullable = false)
	private int itemOrder;

	@Column(nullable = false, length = 100)
	private String counterparty;

	@Column(name = "original_amount", nullable = false)
	private long originalAmount;

	@Column(name = "personal_amount", nullable = false)
	private long personalAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_type", nullable = false, length = 30)
	private com.goodbuy.backend.analysis.domain.TransactionType transactionType;

	@Enumerated(EnumType.STRING)
	@Column(name = "purpose_category", nullable = false, length = 30)
	private com.goodbuy.backend.analysis.domain.PurposeCategory purposeCategory;

	@Enumerated(EnumType.STRING)
	@Column(name = "merchant_type", nullable = false, length = 30)
	private com.goodbuy.backend.analysis.domain.MerchantType merchantType;

	@Column(name = "decision_source", nullable = false, length = 20)
	private String decisionSource;

	@Column(nullable = false)
	private boolean anomaly;

	@Enumerated(EnumType.STRING)
	@Column(name = "anomaly_reason", nullable = false, length = 50)
	private com.goodbuy.backend.analysis.domain.AnomalyReason anomalyReason;

	@Column(name = "anomaly_detail", length = 300)
	private String anomalyDetail;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ExpenseTransactionEntity() {
	}

	ExpenseTransactionEntity(
			UUID id,
			AnalysisEntity analysis,
			int itemOrder,
			ClassifiedTransaction transaction,
			Instant now) {
		this.id = id;
		this.analysis = analysis;
		this.itemOrder = itemOrder;
		this.counterparty = transaction.counterparty();
		this.originalAmount = transaction.originalAmount();
		this.personalAmount = transaction.personalAmount();
		this.transactionType = transaction.transactionType();
		this.purposeCategory = transaction.purposeCategory();
		this.merchantType = transaction.merchantType();
		this.decisionSource = "SYSTEM";
		this.anomaly = transaction.anomaly();
		this.anomalyReason = transaction.anomalyReason();
		this.anomalyDetail = transaction.anomalyDetail();
		this.createdAt = now;
		this.updatedAt = now;
	}
}
