package com.goodbuy.backend.ocr;

import java.util.Arrays;

public record OcrRequest(
		String originalFilename,
		String contentType,
		byte[] content) {

	public OcrRequest {
		content = Arrays.copyOf(content, content.length);
	}

	@Override
	public byte[] content() {
		return Arrays.copyOf(content, content.length);
	}
}
