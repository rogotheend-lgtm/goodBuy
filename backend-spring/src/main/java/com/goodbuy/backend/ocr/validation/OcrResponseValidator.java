package com.goodbuy.backend.ocr.validation;

import com.goodbuy.backend.ocr.dto.OcrParsedResponse;
import com.goodbuy.backend.ocr.dto.OcrSummary;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/** Python 응답의 필수값과 summary 합계가 실제 거래 목록과 일치하는지 검증합니다. */
@Component
public class OcrResponseValidator {

	private final Validator validator;

	public OcrResponseValidator(Validator validator) {
		this.validator = validator;
	}

	public OcrSummary validateAndRecalculate(OcrParsedResponse response) {
		if (response == null) {
			throw new InvalidOcrResponseException("OCR response must not be null");
		}

		Set<ConstraintViolation<OcrParsedResponse>> violations = validator.validate(response);
		if (!violations.isEmpty()) {
			String details = violations.stream()
					.sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
					.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
					.collect(Collectors.joining(", "));
			throw new InvalidOcrResponseException("Invalid OCR response: " + details);
		}

		long calculatedAmount;
		try {
			// Python이 보낸 summary를 그대로 신뢰하지 않고 Spring에서 다시 더합니다.
			calculatedAmount = response.transactions().stream()
					.mapToLong(transaction -> transaction.amount())
					.reduce(0L, Math::addExact);
		} catch (ArithmeticException exception) {
			throw new InvalidOcrResponseException("OCR transaction amount sum overflowed", exception);
		}

		OcrSummary calculatedSummary = new OcrSummary(response.transactions().size(), calculatedAmount);
		if (!calculatedSummary.equals(response.summary())) {
			throw new InvalidOcrResponseException(
					"OCR summary does not match transactions: expected " + calculatedSummary
							+ ", received " + response.summary());
		}

		return calculatedSummary;
	}
}
