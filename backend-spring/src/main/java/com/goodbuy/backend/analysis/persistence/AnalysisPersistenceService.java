package com.goodbuy.backend.analysis.persistence;

import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.domain.TransactionType;
import com.goodbuy.backend.common.InvalidRequestException;
import com.goodbuy.backend.common.ResourceNotFoundException;
import com.goodbuy.backend.session.AnonymousSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AnalysisPersistenceService {

	private final AnalysisRepository analysisRepository;
	private final ExpenseTransactionRepository transactionRepository;
	private final AnonymousSessionRepository sessionRepository;
	private final Clock clock;

	@Autowired
	public AnalysisPersistenceService(
			AnalysisRepository analysisRepository,
			ExpenseTransactionRepository transactionRepository,
			AnonymousSessionRepository sessionRepository) {
		this(analysisRepository, transactionRepository, sessionRepository, Clock.systemUTC());
	}

	AnalysisPersistenceService(
			AnalysisRepository analysisRepository,
			ExpenseTransactionRepository transactionRepository,
			AnonymousSessionRepository sessionRepository,
			Clock clock) {
		this.analysisRepository = analysisRepository;
		this.transactionRepository = transactionRepository;
		this.sessionRepository = sessionRepository;
		this.clock = clock;
	}

	@Transactional
	public UUID createPending(UUID sessionId) {
		var session = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Anonymous session was not found"));
		AnalysisEntity analysis = new AnalysisEntity(UUID.randomUUID(), session, clock.instant());
		analysis.markProcessing();
		return analysisRepository.save(analysis).getId();
	}

	@Transactional
	public StoredAnalysis complete(UUID analysisId, List<ClassifiedTransaction> transactions) {
		AnalysisEntity analysis = requireAnalysis(analysisId);
		Instant now = clock.instant();

		List<ExpenseTransactionEntity> entities = java.util.stream.IntStream.range(0, transactions.size())
				.mapToObj(index -> new ExpenseTransactionEntity(
						UUID.randomUUID(), analysis, index, transactions.get(index), now))
				.toList();
		transactionRepository.saveAll(entities);
		analysis.markCompleted(now);

		return toStoredAnalysis(analysis, entities);
	}

	@Transactional
	public void fail(UUID analysisId, String reason) {
		AnalysisEntity analysis = requireAnalysis(analysisId);
		analysis.markFailed(limit(reason, 500), clock.instant());
	}

	@Transactional(readOnly = true)
	public StoredAnalysis getOwned(UUID analysisId, UUID sessionId) {
		AnalysisEntity analysis = analysisRepository.findByIdAndAnonymousSession_Id(analysisId, sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Analysis was not found"));
		return toStoredAnalysis(
				analysis,
				transactionRepository.findAllByAnalysis_IdOrderByItemOrderAsc(analysisId));
	}

	@Transactional
	public UUID applyUserDecision(
			UUID transactionId,
			UUID sessionId,
			TransactionType transactionType,
			PurposeCategory purposeCategory,
			Long requestedPersonalAmount) {
		if (transactionType == TransactionType.NEEDS_REVIEW) {
			throw new InvalidRequestException("A user decision cannot remain NEEDS_REVIEW");
		}

		ExpenseTransactionEntity transaction = transactionRepository
				.findByIdAndAnalysis_AnonymousSession_Id(transactionId, sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction was not found"));

		long personalAmount = switch (transactionType) {
			case EXPENSE -> requestedPersonalAmount == null
					? transaction.getOriginalAmount()
					: requestedPersonalAmount;
			case SELF_TRANSFER, OTHER_PERSON -> 0;
			case NEEDS_REVIEW -> throw new IllegalStateException("Already rejected NEEDS_REVIEW");
		};

		if (personalAmount < 0 || personalAmount > transaction.getOriginalAmount()) {
			throw new InvalidRequestException("Personal amount must be between 0 and original amount");
		}
		if (transactionType == TransactionType.EXPENSE && personalAmount == 0) {
			throw new InvalidRequestException("An expense personal amount must be greater than 0");
		}

		PurposeCategory resolvedCategory = transactionType == TransactionType.EXPENSE
				? purposeCategory == null ? transaction.getPurposeCategory() : purposeCategory
				: PurposeCategory.OTHER;
		transaction.applyUserDecision(transactionType, resolvedCategory, personalAmount, clock.instant());
		return transaction.getAnalysisId();
	}

	private AnalysisEntity requireAnalysis(UUID analysisId) {
		return analysisRepository.findById(analysisId)
				.orElseThrow(() -> new ResourceNotFoundException("Analysis was not found"));
	}

	private StoredAnalysis toStoredAnalysis(
			AnalysisEntity analysis,
			List<ExpenseTransactionEntity> transactions) {
		return new StoredAnalysis(
				analysis.getId(),
				analysis.getStatus(),
				analysis.getFailureReason(),
				analysis.getCreatedAt(),
				analysis.getCompletedAt(),
				transactions.stream()
						.map(transaction -> new StoredTransaction(transaction.getId(), transaction.toDomain()))
						.toList());
	}

	private String limit(String value, int maxLength) {
		if (value == null) {
			return "Unknown OCR failure";
		}
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}
}
