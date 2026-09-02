package com.goodbuy.backend.ocr.validation;

import com.goodbuy.backend.ocr.dto.OcrParsedResponse;
import com.goodbuy.backend.ocr.dto.OcrSummary;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

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
