package com.goodbuy.backend.analysis.api;

import com.goodbuy.backend.common.InvalidRequestException;
import com.goodbuy.backend.ocr.OcrRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class AnalysisRequestValidator {

	private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;
	private static final int MAX_IMAGE_COUNT = 5;
	private static final Set<String> SUPPORTED_MEDIA_TYPES = Set.of(
			MediaType.IMAGE_PNG_VALUE,
			MediaType.IMAGE_JPEG_VALUE);

	public List<OcrRequest> validateAndConvert(List<MultipartFile> images, String ownerName) {
		validateOwnerName(ownerName);
		if (images == null || images.isEmpty()) {
			throw new InvalidRequestException("At least one image is required");
		}
		if (images.size() > MAX_IMAGE_COUNT) {
			throw new InvalidRequestException("A maximum of 5 images is allowed");
		}
		return images.stream()
				.map(this::validateAndConvertImage)
				.toList();
	}

	private OcrRequest validateAndConvertImage(MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw new InvalidRequestException("Images must not be empty");
		}
		if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
			throw new InvalidRequestException("Image must not exceed 10MB");
		}
		if (!SUPPORTED_MEDIA_TYPES.contains(image.getContentType())) {
			throw new InvalidRequestException("Only PNG and JPEG images are supported");
		}

		try {
			byte[] content = image.getBytes();
			if (!hasValidSignature(content, image.getContentType())) {
				throw new InvalidRequestException("Image content does not match its media type");
			}
			String filename = image.getOriginalFilename() == null || image.getOriginalFilename().isBlank()
					? "upload-image"
					: image.getOriginalFilename();
			return new OcrRequest(filename, image.getContentType(), content);
		} catch (IOException exception) {
			throw new InvalidRequestException("Image could not be read");
		}
	}

	private void validateOwnerName(String ownerName) {
		if (ownerName == null || ownerName.isBlank()) {
			throw new InvalidRequestException("Owner name is required");
		}
		String nameWithoutWhitespace = ownerName.replaceAll("\\s+", "");
		int normalizedLength = nameWithoutWhitespace.codePointCount(0, nameWithoutWhitespace.length());
		if (normalizedLength < 2 || normalizedLength > 30) {
			throw new InvalidRequestException("Owner name must contain between 2 and 30 characters");
		}
	}

	private boolean hasValidSignature(byte[] content, String contentType) {
		if (MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
			return content.length >= 8
					&& (content[0] & 0xFF) == 0x89
					&& content[1] == 0x50
					&& content[2] == 0x4E
					&& content[3] == 0x47
					&& content[4] == 0x0D
					&& content[5] == 0x0A
					&& content[6] == 0x1A
					&& content[7] == 0x0A;
		}
		return content.length >= 3
				&& (content[0] & 0xFF) == 0xFF
				&& (content[1] & 0xFF) == 0xD8
				&& (content[2] & 0xFF) == 0xFF;
	}
}
