package com.goodbuy.backend.analysis.persistence;

import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.analysis.domain.DecisionSource;
import com.goodbuy.backend.analysis.domain.MerchantType;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.domain.ReviewReason;
import com.goodbuy.backend.analysis.domain.TransactionType;
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
@Table(name = "expense_transaction")
public class ExpenseTransactionEntity {

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
	private TransactionType transactionType;

	@Enumerated(EnumType.STRING)
	@Column(name = "purpose_category", nullable = false, length = 30)
	private PurposeCategory purposeCategory;

	@Enumerated(EnumType.STRING)
	@Column(name = "merchant_type", nullable = false, length = 30)
	private MerchantType merchantType;

	@Enumerated(EnumType.STRING)
	@Column(name = "decision_source", nullable = false, length = 20)
	private DecisionSource decisionSource;

	@Column(name = "requires_review", nullable = false)
	private boolean requiresReview;

	@Enumerated(EnumType.STRING)
	@Column(name = "review_reason", nullable = false, length = 50)
	private ReviewReason reviewReason;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ExpenseTransactionEntity() {
	}

	public ExpenseTransactionEntity(
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
		this.decisionSource = transaction.decisionSource();
		this.requiresReview = transaction.requiresReview();
		this.reviewReason = transaction.reviewReason();
		this.createdAt = now;
		this.updatedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public UUID getAnalysisId() {
		return analysis.getId();
	}

	public int getItemOrder() {
		return itemOrder;
	}

	public long getOriginalAmount() {
		return originalAmount;
	}

	public PurposeCategory getPurposeCategory() {
		return purposeCategory;
	}

	public ClassifiedTransaction toDomain() {
		return new ClassifiedTransaction(
				counterparty,
				originalAmount,
				personalAmount,
				transactionType,
				purposeCategory,
				merchantType,
				decisionSource,
				requiresReview,
				reviewReason);
	}

	public void applyUserDecision(
			TransactionType transactionType,
			PurposeCategory purposeCategory,
			long personalAmount,
			Instant now) {
		this.transactionType = transactionType;
		this.purposeCategory = purposeCategory;
		this.personalAmount = personalAmount;
		this.decisionSource = DecisionSource.USER;
		this.requiresReview = false;
		this.reviewReason = ReviewReason.NONE;
		this.updatedAt = now;
	}
}
