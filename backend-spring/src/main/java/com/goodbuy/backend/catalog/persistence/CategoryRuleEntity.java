package com.goodbuy.backend.catalog.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** 가맹점명 분류에 사용할 키워드 규칙을 읽기 전용으로 매핑합니다. */
@Entity
@Immutable
@Table(name = "category_rules")
public class CategoryRuleEntity {

	@Id
	@Column(name = "rule_id", nullable = false)
	private Integer ruleId;

	@Column(name = "category_id", nullable = false)
	private Integer categoryId;

	@Column(name = "keyword", nullable = false, length = 50)
	private String keyword;

	protected CategoryRuleEntity() {
	}

	public Integer getRuleId() {
		return ruleId;
	}

	public Integer getCategoryId() {
		return categoryId;
	}

	public String getKeyword() {
		return keyword;
	}
}
