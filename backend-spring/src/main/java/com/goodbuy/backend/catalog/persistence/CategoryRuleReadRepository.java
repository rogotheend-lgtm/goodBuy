package com.goodbuy.backend.catalog.persistence;

import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** 카테고리와 키워드를 한 번의 읽기 전용 쿼리로 가져옵니다. */
public interface CategoryRuleReadRepository extends Repository<CategoryRuleEntity, Integer> {

	@Query(value = """
			SELECT r.rule_id AS "ruleId",
			       r.keyword AS keyword,
			       c.category_id AS "categoryId",
			       c.category_name AS "categoryName",
			       c.dutch_threshold AS "dutchThreshold",
			       c.gif_url AS "gifUrl"
			FROM categories c
			LEFT JOIN category_rules r ON r.category_id = c.category_id
			ORDER BY length(r.keyword) DESC NULLS LAST, r.rule_id ASC, c.category_id ASC
			""", nativeQuery = true)
	List<CategoryMappingRow> findAllCategoryMappings();
}
