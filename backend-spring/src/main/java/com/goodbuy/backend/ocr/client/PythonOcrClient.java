package com.goodbuy.backend.ocr.client;

import com.goodbuy.backend.ocr.OcrPort;
import com.goodbuy.backend.ocr.OcrRequest;
import com.goodbuy.backend.ocr.dto.OcrParsedResponse;
import com.goodbuy.backend.ocr.validation.OcrResponseValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "goodbuy.ocr.mode", havingValue = "python")
public class PythonOcrClient implements OcrPort {

	private final RestClient restClient;
	private final OcrClientProperties properties;
	private final OcrResponseValidator responseValidator;

	public PythonOcrClient(
			@Qualifier("pythonOcrRestClient") RestClient restClient,
			OcrClientProperties properties,
			OcrResponseValidator responseValidator) {
		this.restClient = restClient;
		this.properties = properties;
		this.responseValidator = responseValidator;
	}

	@Override
	public OcrParsedResponse parse(OcrRequest request) {
		try {
			OcrParsedResponse response = restClient.post()
					.uri(properties.parsePath())
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(createMultipartBody(request))
					.retrieve()
					.body(OcrParsedResponse.class);

			if (response == null) {
				throw new OcrServiceException("Python OCR returned an empty response");
			}

			responseValidator.validateAndRecalculate(response);
			return response;
		} catch (OcrServiceException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new OcrServiceException("Python OCR request failed", exception);
		}
	}

	private MultiValueMap<String, Object> createMultipartBody(OcrRequest request) {
		ByteArrayResource imageResource = new ByteArrayResource(request.content()) {
			@Override
			public String getFilename() {
				return request.originalFilename();
			}
		};

		HttpHeaders imageHeaders = new HttpHeaders();
		imageHeaders.setContentType(MediaType.parseMediaType(request.contentType()));

		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("image", new HttpEntity<>(imageResource, imageHeaders));
		return parts;
	}
}
