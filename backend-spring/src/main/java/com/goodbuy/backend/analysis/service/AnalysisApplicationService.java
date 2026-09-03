package com.goodbuy.backend.analysis.service;

import com.goodbuy.backend.analysis.api.AnalysisResponse;
import com.goodbuy.backend.analysis.api.TransactionResponse;
import com.goodbuy.backend.analysis.domain.AnalysisSummary;
import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.analysis.persistence.AnalysisPersistenceService;
import com.goodbuy.backend.ocr.OcrPort;
import com.goodbuy.backend.ocr.OcrRequest;
import com.goodbuy.backend.ocr.validation.OcrResponseValidator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * OCR 호출부터 거래 분류와 응답 생성까지 한 번의 분석 흐름을 조정합니다.
 * 각 세부 규칙은 전용 컴포넌트에 맡기고 처리 순서만 책임집니다.
 */
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

	public AnalysisResponse analyze(String ownerName, List<OcrRequest> requests) {
		List<ClassifiedTransaction> classifiedTransactions = new ArrayList<>();

		// Python은 단일 이미지만 받으므로 업로드 순서대로 한 장씩 동기 호출합니다.
		for (OcrRequest request : requests) {
			var ocrResponse = ocrPort.parse(request);
			ocrResponseValidator.validateAndRecalculate(ocrResponse);
			ocrResponse.transactions().stream()
					.map(transaction -> transactionClassifier.classify(ownerName, transaction))
					.forEach(classifiedTransactions::add);
		}

		// 프론트 응답과 DB 저장에 같은 최종 분류 결과를 사용합니다.
		List<TransactionResponse> transactions = classifiedTransactions.stream()
				.map(TransactionResponse::from)
				.toList();
		AnalysisSummary summary = summaryCalculator.calculate(classifiedTransactions);
		persistenceService.save(classifiedTransactions);

		return new AnalysisResponse(transactions, summary);
	}
}
