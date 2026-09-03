package com.goodbuy.backend.analysis.persistence;

import com.goodbuy.backend.analysis.domain.ClassifiedTransaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/** 분석 결과를 내부 UUID로 저장하되 ID를 프론트엔드에 노출하지 않습니다. */
@Service
public class AnalysisPersistenceService {

	private final AnalysisRepository analysisRepository;
	private final ExpenseTransactionRepository transactionRepository;
	private final Clock clock;

	public AnalysisPersistenceService(
			AnalysisRepository analysisRepository,
			ExpenseTransactionRepository transactionRepository) {
		this.analysisRepository = analysisRepository;
		this.transactionRepository = transactionRepository;
		this.clock = Clock.systemUTC();
	}

	@Transactional
	public void save(List<ClassifiedTransaction> transactions) {
		Instant now = clock.instant();
		AnalysisEntity analysis = analysisRepository.save(new AnalysisEntity(UUID.randomUUID(), now));

		List<ExpenseTransactionEntity> entities = IntStream.range(0, transactions.size())
				.mapToObj(index -> new ExpenseTransactionEntity(
						UUID.randomUUID(), analysis, index, transactions.get(index), now))
				.toList();
		transactionRepository.saveAll(entities);
	}
}
