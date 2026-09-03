package com.goodbuy.backend.catalog.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Supabase가 관리하는 소비 카테고리 기준을 읽기 전용으로 매핑합니다. */
@Entity
@Immutable
@Table(name = "categories")
public class CategoryEntity {

	@Id
	@Column(name = "category_id", nullable = false)
	private Integer categoryId;

	@Column(name = "category_name", nullable = false, length = 30)
	private String categoryName;

	@Column(name = "dutch_threshold", nullable = false)
	private Integer dutchThreshold;

	@Column(name = "gif_url", nullable = false, length = 255)
	private String gifUrl;

	protected CategoryEntity() {
	}

	public Integer getCategoryId() {
		return categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public Integer getDutchThreshold() {
		return dutchThreshold;
	}

	public String getGifUrl() {
		return gifUrl;
	}
}
