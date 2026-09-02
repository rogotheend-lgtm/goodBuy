package com.goodbuy.backend.ocr.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OcrParsedResponse(
		@NotNull List<@Valid OcrTransactionItem> transactions,
		@NotNull @Valid OcrSummary summary) {
}
