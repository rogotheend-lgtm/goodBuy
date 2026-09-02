package com.goodbuy.backend.ocr.client;

import com.goodbuy.backend.ocr.OcrPort;
import com.goodbuy.backend.ocr.OcrRequest;
import com.goodbuy.backend.ocr.dto.OcrParsedResponse;
import com.goodbuy.backend.ocr.dto.OcrSummary;
import com.goodbuy.backend.ocr.dto.OcrTransactionItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "goodbuy.ocr.mode", havingValue = "mock", matchIfMissing = true)
public class MockOcrClient implements OcrPort {

	private static final List<OcrTransactionItem> SAMPLE_TRANSACTIONS = List.of(
			new OcrTransactionItem("세븐일레븐 광주산정고려점", 16_480),
			new OcrTransactionItem("세븐일레븐광주소촌이지점", 2_750),
			new OcrTransactionItem("버거앤타코", 11_600),
			new OcrTransactionItem("벌크커피하남소촌점", 3_000),
			new OcrTransactionItem("맘스터치소촌점", 7_900),
			new OcrTransactionItem("금호칼국수", 7_500),
			new OcrTransactionItem("토스페이_TOSS", 630),
			new OcrTransactionItem("토스페이_TOSS", 620),
			new OcrTransactionItem("다이소", 6_000),
			new OcrTransactionItem("389마트", 1_200));

	@Override
	public OcrParsedResponse parse(OcrRequest request) {
		return new OcrParsedResponse(
				SAMPLE_TRANSACTIONS,
				new OcrSummary(SAMPLE_TRANSACTIONS.size(), 57_680));
	}
}
