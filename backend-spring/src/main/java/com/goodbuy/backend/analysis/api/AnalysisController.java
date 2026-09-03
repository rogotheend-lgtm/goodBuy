package com.goodbuy.backend.analysis.api;

import com.goodbuy.backend.analysis.service.AnalysisApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 프론트엔드가 호출하는 소비 분석 API의 시작점입니다.
 * HTTP 입력만 처리하고, 실제 분석은 서비스에 맡깁니다.
 */
@RestController
@RequestMapping("/api/v1/analyses")
@Tag(name = "소비 분석", description = "거래내역 이미지를 한 번에 분석해 결과를 반환하는 API")
public class AnalysisController {

	private final AnalysisRequestValidator requestValidator;
	private final AnalysisApplicationService analysisService;

	public AnalysisController(
			AnalysisRequestValidator requestValidator,
			AnalysisApplicationService analysisService) {
		this.requestValidator = requestValidator;
		this.analysisService = analysisService;
	}

	@Operation(
			summary = "거래내역 이미지 분석",
			description = "PNG 또는 JPEG 이미지 1~5장과 계좌 소유자 이름을 받아 이미지를 순서대로 OCR 처리한 뒤 "
					+ "요청과 결과를 저장하지 않고 프론트엔드에 즉시 반환합니다. 감지된 이상치는 이유와 상세 설명만 "
					+ "출력하고 사용자 재입력을 요구하지 않습니다.")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "분석 완료",
					content = @Content(schema = @Schema(implementation = AnalysisResponse.class))),
			@ApiResponse(responseCode = "400", description = "잘못된 이름 또는 이미지", content = @Content),
			@ApiResponse(responseCode = "413", description = "파일당 10MB 또는 요청 전체 50MB 초과", content = @Content),
			@ApiResponse(responseCode = "502", description = "Python OCR 호출 또는 응답 검증 실패", content = @Content),
			@ApiResponse(responseCode = "503", description = "DB 카테고리 기준 조회 실패", content = @Content)
	})
	@PostMapping(
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AnalysisResponse> analyze(
			@Parameter(description = "거래내역 PNG/JPEG 이미지 1~5장, 파일당 최대 10MB", required = true)
			@RequestPart("images") List<MultipartFile> images,
			@Parameter(description = "계좌 소유자 이름. 자가 이체 판별에만 사용하며 DB에는 저장하지 않습니다.", required = true, example = "강병호")
			@RequestPart("ownerName") String ownerName) {
		// 1. 업로드 파일을 검사하고 Python OCR에 전달할 형태로 바꿉니다.
		var requests = requestValidator.validateAndConvert(images, ownerName);

		// 2. OCR과 거래 분류가 끝난 결과를 저장하지 않고 바로 반환합니다.
		return ResponseEntity.ok(analysisService.analyze(ownerName, requests));
	}
}
