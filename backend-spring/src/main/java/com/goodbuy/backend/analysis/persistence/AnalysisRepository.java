package com.goodbuy.backend.analysis.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AnalysisRepository extends JpaRepository<AnalysisEntity, UUID> {
}
