package com.goodbuy.backend.analysis.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseTransactionRepository extends JpaRepository<ExpenseTransactionEntity, UUID> {

	List<ExpenseTransactionEntity> findAllByAnalysis_IdOrderByItemOrderAsc(UUID analysisId);

	Optional<ExpenseTransactionEntity> findByIdAndAnalysis_AnonymousSession_Id(UUID id, UUID anonymousSessionId);
}
