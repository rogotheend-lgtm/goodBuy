package com.goodbuy.backend.ocr.client;

public class OcrServiceException extends RuntimeException {

	public OcrServiceException(String message) {
		super(message);
	}

	public OcrServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
