package com.goodbuy.backend.ocr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;

public record OcrSummary(
		@JsonProperty("total_count") @PositiveOrZero int totalCount,
		@JsonProperty("total_amount") @PositiveOrZero long totalAmount) {
}
