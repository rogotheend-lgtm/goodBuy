package com.goodbuy.backend.common;

import com.goodbuy.backend.catalog.CategoryCatalogUnavailableException;
import com.goodbuy.backend.ocr.client.OcrServiceException;
import com.goodbuy.backend.ocr.validation.InvalidOcrResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(InvalidRequestException.class)
	ResponseEntity<ProblemDetail> handleInvalidRequest(InvalidRequestException exception) {
		return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
	}

	@ExceptionHandler({OcrServiceException.class, InvalidOcrResponseException.class})
	ResponseEntity<ProblemDetail> handleOcrFailure(RuntimeException exception) {
		return problem(HttpStatus.BAD_GATEWAY, "OCR_SERVICE_ERROR", exception.getMessage());
	}

	@ExceptionHandler(CategoryCatalogUnavailableException.class)
	ResponseEntity<ProblemDetail> handleCategoryCatalogFailure(CategoryCatalogUnavailableException exception) {
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "CATEGORY_CATALOG_UNAVAILABLE", exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> handleBeanValidation(MethodArgumentNotValidException exception) {
		return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request fields are invalid");
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ResponseEntity<ProblemDetail> handleMaxUpload(MaxUploadSizeExceededException exception) {
		return problem(HttpStatus.CONTENT_TOO_LARGE, "IMAGE_TOO_LARGE", "Each image must not exceed 10MB and the request must not exceed 50MB");
	}

	private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(status.getReasonPhrase());
		problem.setProperty("code", code);
		return ResponseEntity.status(status).body(problem);
	}
}
