package com.goodbuy.backend.analysis.service;

import com.goodbuy.backend.analysis.api.AnalysisResponse;
import com.goodbuy.backend.analysis.api.DominantCategoryResponse;
import com.goodbuy.backend.analysis.api.TransactionResponse;
import com.goodbuy.backend.analysis.domain.AnalysisSummary;
import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import com.goodbuy.backend.catalog.CategoryCatalogService;
import com.goodbuy.backend.ocr.OcrPort;
import com.goodbuy.backend.ocr.OcrRequest;
import com.goodbuy.backend.ocr.dto.OcrTransactionItem;
import com.goodbuy.backend.ocr.validation.OcrResponseValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * OCR 호출부터 거래 분류와 응답 생성까지 한 번의 분석 흐름을 조정합니다.
 * 각 세부 규칙은 전용 컴포넌트에 맡기고 처리 순서만 책임집니다.
 */
@Service
public class AnalysisApplicationService {
	private static final Logger log = LoggerFactory.getLogger(AnalysisApplicationService.class);

	private final OcrPort ocrPort;
	private final OcrResponseValidator ocrResponseValidator;
	private final TransactionClassifier transactionClassifier;
	private final AnalysisSummaryCalculator summaryCalculator;
	private final DominantCategoryCalculator dominantCategoryCalculator;
	private final CategoryCatalogService categoryCatalogService;

	public AnalysisApplicationService(
			OcrPort ocrPort,
			OcrResponseValidator ocrResponseValidator,
			TransactionClassifier transactionClassifier,
			AnalysisSummaryCalculator summaryCalculator,
			DominantCategoryCalculator dominantCategoryCalculator,
			CategoryCatalogService categoryCatalogService) {
		this.ocrPort = ocrPort;
		this.ocrResponseValidator = ocrResponseValidator;
		this.transactionClassifier = transactionClassifier;
		this.summaryCalculator = summaryCalculator;
		this.dominantCategoryCalculator = dominantCategoryCalculator;
		this.categoryCatalogService = categoryCatalogService;
	}

	public AnalysisResponse analyze(String ownerName, List<OcrRequest> requests) {
		long analysisStartedAt = System.nanoTime();
		log.info("Expense analysis started: imageCount={}", requests.size());
		List<OcrTransactionItem> parsedTransactions = new ArrayList<>();

		// 1. Python은 단일 이미지만 받으므로 업로드 순서대로 한 장씩 동기 호출합니다.
		for (int index = 0; index < requests.size(); index++) {
			long ocrStartedAt = System.nanoTime();
			try {
				var ocrResponse = ocrPort.parse(requests.get(index));
				ocrResponseValidator.validateAndRecalculate(ocrResponse);
				parsedTransactions.addAll(ocrResponse.transactions());
				log.info(
						"Python OCR completed: imageIndex={}, transactionCount={}, durationMs={}",
						index + 1,
						ocrResponse.transactions().size(),
						elapsedMillis(ocrStartedAt));
			} catch (RuntimeException exception) {
				log.warn(
						"Python OCR failed: imageIndex={}, durationMs={}, error={}",
						index + 1,
						elapsedMillis(ocrStartedAt),
						exception.getMessage());
				throw exception;
			}
		}

		// 2. OCR이 끝나면 DB에서 카테고리 맵을 한 번 조회하고 Spring이 직접 분류합니다.
		long categoryStartedAt = System.nanoTime();
		var categoryCatalog = categoryCatalogService.loadCatalog();
		var categoryMappings = categoryCatalog.mappings();
		log.info(
				"Category catalog loaded: source={}, mappingCount={}, durationMs={}",
				categoryCatalog.source(),
				categoryMappings.size(),
				elapsedMillis(categoryStartedAt));
		List<ClassifiedTransaction> classifiedTransactions = parsedTransactions.stream()
				.map(transaction -> transactionClassifier.classify(ownerName, transaction, categoryMappings))
				.toList();

		// 3. 요청 결과는 저장하지 않고, 이 응답을 프론트엔드에 한 번 반환합니다.
		List<TransactionResponse> transactions = classifiedTransactions.stream()
				.map(TransactionResponse::from)
				.toList();
		AnalysisSummary summary = summaryCalculator.calculate(classifiedTransactions);
		var dominantCategory = dominantCategoryCalculator
				.calculate(classifiedTransactions, summary.expenseAmount())
				.orElse(null);

		log.info(
				"Expense analysis completed: imageCount={}, transactionCount={}, durationMs={}",
				requests.size(),
				transactions.size(),
				elapsedMillis(analysisStartedAt));
		return new AnalysisResponse(
				transactions,
				summary,
				categoryCatalog.source(),
				DominantCategoryResponse.from(dominantCategory));
	}

	private long elapsedMillis(long startedAt) {
		return (System.nanoTime() - startedAt) / 1_000_000;
	}
}
