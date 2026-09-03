package com.goodbuy.backend.catalog;

import com.goodbuy.backend.analysis.domain.PurposeCategory;
import com.goodbuy.backend.analysis.service.NameNormalizer;
import com.goodbuy.backend.catalog.persistence.CategoryMappingRow;
import com.goodbuy.backend.catalog.persistence.CategoryRuleReadRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 분석 요청마다 Supabase의 카테고리 맵을 읽어 Spring 분류용 값으로 변환합니다. */
@Service
public class CategoryCatalogService {

	private static final long[] RETRY_DELAYS_MILLIS = {0, 250, 750, 1_500};
	private static final String UNAVAILABLE_MESSAGE =
			"카테고리 기준 정보를 불러오지 못했습니다. 잠시 후 다시 분석해주세요.";

	private final CategoryRuleReadRepository repository;
	private final NameNormalizer normalizer;

	public CategoryCatalogService(CategoryRuleReadRepository repository, NameNormalizer normalizer) {
		this.repository = repository;
		this.normalizer = normalizer;
	}

	public List<CategoryMapping> loadMappings() {
		DataAccessException lastFailure = null;

		for (long delayMillis : RETRY_DELAYS_MILLIS) {
			waitBeforeRetry(delayMillis);
			try {
				List<CategoryMapping> mappings = convert(repository.findAllCategoryMappings());
				if (containsOtherCategory(mappings)) {
					return mappings;
				}
			} catch (DataAccessException exception) {
				lastFailure = exception;
			}
		}

		if (lastFailure != null) {
			throw new CategoryCatalogUnavailableException(UNAVAILABLE_MESSAGE, lastFailure);
		}
		throw new CategoryCatalogUnavailableException(UNAVAILABLE_MESSAGE);
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

	private void waitBeforeRetry(long delayMillis) {
		if (delayMillis == 0) {
			return;
		}
		try {
			Thread.sleep(delayMillis);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new CategoryCatalogUnavailableException(UNAVAILABLE_MESSAGE, exception);
		}
	}
}
