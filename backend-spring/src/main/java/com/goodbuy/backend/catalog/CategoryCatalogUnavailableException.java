package com.goodbuy.backend.catalog;

/** DB 카테고리 기준이 일시적으로 비어 있거나 조회할 수 없을 때 사용합니다. */
public class CategoryCatalogUnavailableException extends RuntimeException {

	public CategoryCatalogUnavailableException(String message) {
		super(message);
	}

	public CategoryCatalogUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
