package com.goodbuy.backend.ocr;

import com.goodbuy.backend.ocr.dto.OcrParsedResponse;

public interface OcrPort {

	OcrParsedResponse parse(OcrRequest request);
}
