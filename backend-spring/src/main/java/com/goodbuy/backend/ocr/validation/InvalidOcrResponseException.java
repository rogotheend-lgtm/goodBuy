package com.goodbuy.backend.ocr.validation;

public class InvalidOcrResponseException extends RuntimeException {

	public InvalidOcrResponseException(String message) {
		super(message);
	}

	public InvalidOcrResponseException(String message, Throwable cause) {
		super(message, cause);
	}
}
