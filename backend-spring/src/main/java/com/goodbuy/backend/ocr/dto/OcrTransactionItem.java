package com.goodbuy.backend.ocr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OcrTransactionItem(
		@NotBlank @Size(max = 100) String counterparty,
		@Positive long amount) {
}
