package com.goodbuy.backend.catalog.persistence;

/** 카테고리 JOIN 조회 결과를 필요한 필드만 받는 projection입니다. */
public interface CategoryMappingRow {

	Integer getRuleId();

	String getKeyword();

	Integer getCategoryId();

	String getCategoryName();

	Integer getDutchThreshold();

	String getGifUrl();
}
