package com.goodbuy.backend.catalog;

import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.service.NameNormalizer;
import com.goodbuy.backend.catalog.persistence.CategoryMappingRow;
import com.goodbuy.backend.catalog.persistence.CategoryRuleReadRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

/** 분석 요청마다 Supabase의 카테고리 맵을 읽어 Spring 분류용 값으로 변환합니다. */
@Service
public class CategoryCatalogService {

	private static final Logger log = LoggerFactory.getLogger(CategoryCatalogService.class);

	private final CategoryRuleReadRepository repository;
	private final NameNormalizer normalizer;
	private final FallbackCategoryCatalog fallbackCatalog;

	public CategoryCatalogService(
			CategoryRuleReadRepository repository,
			NameNormalizer normalizer,
			FallbackCategoryCatalog fallbackCatalog) {
		this.repository = repository;
		this.normalizer = normalizer;
		this.fallbackCatalog = fallbackCatalog;
	}

	public CategoryCatalogSnapshot loadCatalog() {
		try {
			List<CategoryMapping> mappings = convert(repository.findAllCategoryMappings());
			if (containsOtherCategory(mappings)) {
				return new CategoryCatalogSnapshot(mappings, CategoryCatalogSource.DATABASE);
			}
			log.warn("Category catalog is empty or has no OTHER category; using fallback catalog");
		} catch (DataAccessException | IllegalStateException exception) {
			log.warn("Category database unavailable; using fallback catalog: {}", exception.getMessage());
		}

		return new CategoryCatalogSnapshot(fallbackCatalog.mappings(), CategoryCatalogSource.FALLBACK);
	}

	private List<CategoryMapping> convert(List<CategoryMappingRow> rows) {
		return rows.stream()
				.map(this::toMapping)
				.sorted(Comparator
						.comparingInt((CategoryMapping mapping) -> keywordLength(mapping.normalizedKeyword()))
						.reversed()
						.thenComparing(mapping -> mapping.ruleId() == null ? Integer.MAX_VALUE : mapping.ruleId())
						.thenComparing(CategoryMapping::categoryId))
				.toList();
	}

	private boolean containsOtherCategory(List<CategoryMapping> mappings) {
		return mappings.stream().anyMatch(mapping -> mapping.purposeCategory() == PurposeCategory.OTHER);
	}

	private CategoryMapping toMapping(CategoryMappingRow row) {
		try {
			return new CategoryMapping(
					row.getRuleId(),
					row.getCategoryId(),
					row.getKeyword() == null ? null : normalizer.normalize(row.getKeyword()),
					PurposeCategory.valueOf(row.getCategoryName()),
					row.getDutchThreshold(),
					row.getGifUrl());
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("Unknown category name from database: " + row.getCategoryName(), exception);
		}
	}

	private int keywordLength(String keyword) {
		return keyword == null ? -1 : keyword.length();
	}

}
