package com.goodbuy.backend.analysis.service;

import com.goodbuy.backend.analysis.api.AnalysisResponse;
import com.goodbuy.backend.analysis.api.TransactionResponse;
import com.goodbuy.backend.analysis.api.TransactionReviewRequest;
import com.goodbuy.backend.analysis.domain.AnalysisSummary;
import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.analysis.persistence.AnalysisPersistenceService;
import com.goodbuy.backend.analysis.persistence.StoredAnalysis;
import com.goodbuy.backend.ocr.OcrPort;
import com.goodbuy.backend.ocr.OcrRequest;
import com.goodbuy.backend.ocr.validation.OcrResponseValidator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AnalysisApplicationService {

	private final OcrPort ocrPort;
	private final OcrResponseValidator ocrResponseValidator;
	private final TransactionClassifier transactionClassifier;
	private final AnalysisSummaryCalculator summaryCalculator;
	private final AnalysisPersistenceService persistenceService;

	public AnalysisApplicationService(
			OcrPort ocrPort,
			OcrResponseValidator ocrResponseValidator,
			TransactionClassifier transactionClassifier,
			AnalysisSummaryCalculator summaryCalculator,
			AnalysisPersistenceService persistenceService) {
		this.ocrPort = ocrPort;
		this.ocrResponseValidator = ocrResponseValidator;
		this.transactionClassifier = transactionClassifier;
		this.summaryCalculator = summaryCalculator;
		this.persistenceService = persistenceService;
	}

	public AnalysisResponse analyze(UUID sessionId, String ownerName, List<OcrRequest> requests) {
		UUID analysisId = persistenceService.createPending(sessionId);
		try {
			List<ClassifiedTransaction> classifiedTransactions = new ArrayList<>();
			for (OcrRequest request : requests) {
				var ocrResponse = ocrPort.parse(request);
				ocrResponseValidator.validateAndRecalculate(ocrResponse);
				ocrResponse.transactions().stream()
						.map(transaction -> transactionClassifier.classify(ownerName, transaction))
						.forEach(classifiedTransactions::add);
			}
			return toResponse(persistenceService.complete(analysisId, classifiedTransactions));
		} catch (RuntimeException exception) {
			persistenceService.fail(analysisId, exception.getMessage());
			throw exception;
		}
	}

	public AnalysisResponse get(UUID analysisId, UUID sessionId) {
		return toResponse(persistenceService.getOwned(analysisId, sessionId));
	}

	public AnalysisResponse review(
			UUID transactionId,
			UUID sessionId,
			TransactionReviewRequest request) {
		UUID analysisId = persistenceService.applyUserDecision(
				transactionId,
				sessionId,
				request.transactionType(),
				request.purposeCategory(),
				request.personalAmount());
		return get(analysisId, sessionId);
	}

	private AnalysisResponse toResponse(StoredAnalysis storedAnalysis) {
		List<TransactionResponse> transactions = storedAnalysis.transactions().stream()
				.map(TransactionResponse::from)
				.toList();
		List<ClassifiedTransaction> domainTransactions = storedAnalysis.transactions().stream()
				.map(storedTransaction -> storedTransaction.transaction())
				.toList();
		AnalysisSummary summary = summaryCalculator.calculate(domainTransactions);

		return new AnalysisResponse(
				storedAnalysis.id(),
				storedAnalysis.status(),
				storedAnalysis.failureReason(),
				storedAnalysis.createdAt(),
				storedAnalysis.completedAt(),
				transactions,
				summary);
	}
}
