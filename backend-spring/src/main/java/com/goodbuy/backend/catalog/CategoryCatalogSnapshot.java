package com.goodbuy.backend.catalog;

import java.util.List;

/** 한 번의 분석 요청에서 일관되게 사용할 카테고리 기준과 출처입니다. */
public record CategoryCatalogSnapshot(
		List<CategoryMapping> mappings,
		CategoryCatalogSource source) {
}
