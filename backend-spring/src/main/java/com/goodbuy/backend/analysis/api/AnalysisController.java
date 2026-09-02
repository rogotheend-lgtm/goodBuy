package com.goodbuy.backend.analysis.api;

import com.goodbuy.backend.analysis.service.AnalysisApplicationService;
import com.goodbuy.backend.session.AnonymousSessionService;
import com.goodbuy.backend.session.SessionProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analyses")
@Tag(name = "소비 분석", description = "거래내역 이미지 분석과 결과 조회 API")
public class AnalysisController {

	private static final String SESSION_COOKIE = "goodbuy_session";

	private final AnalysisRequestValidator requestValidator;
	private final AnonymousSessionService sessionService;
	private final SessionProperties sessionProperties;
	private final AnalysisApplicationService analysisService;

	public AnalysisController(
			AnalysisRequestValidator requestValidator,
			AnonymousSessionService sessionService,
			SessionProperties sessionProperties,
			AnalysisApplicationService analysisService) {
		this.requestValidator = requestValidator;
		this.sessionService = sessionService;
		this.sessionProperties = sessionProperties;
		this.analysisService = analysisService;
	}

	@Operation(
			summary = "거래내역 이미지 분석",
			description = "PNG 또는 JPEG 이미지 1~5장과 계좌 소유자 이름을 받아 이미지를 순서대로 OCR 처리한 뒤 "
					+ "결과를 하나로 합쳐 분류하고 저장합니다. "
					+ "최초 호출이면 goodbuy_session 익명 세션 쿠키가 발급됩니다.")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "분석 완료",
					content = @Content(schema = @Schema(implementation = AnalysisResponse.class))),
			@ApiResponse(responseCode = "400", description = "잘못된 이름 또는 이미지", content = @Content),
			@ApiResponse(responseCode = "413", description = "파일당 10MB 또는 요청 전체 50MB 초과", content = @Content),
			@ApiResponse(responseCode = "502", description = "Python OCR 호출 또는 응답 검증 실패", content = @Content)
	})
	@PostMapping(
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AnalysisResponse> analyze(
			@Parameter(description = "거래내역 PNG/JPEG 이미지 1~5장, 파일당 최대 10MB", required = true)
			@RequestPart("images") List<MultipartFile> images,
			@Parameter(description = "계좌 소유자 이름. 자가 이체 판별에만 사용하며 DB에는 저장하지 않습니다.", required = true, example = "강병호")
			@RequestPart("ownerName") String ownerName,
			@Parameter(hidden = true)
			@CookieValue(name = SESSION_COOKIE, required = false) String sessionToken,
			@Parameter(hidden = true)
			HttpServletResponse servletResponse) {
		var requests = requestValidator.validateAndConvert(images, ownerName);
		var session = sessionService.resolveOrCreate(sessionToken);
		servletResponse.addHeader(HttpHeaders.SET_COOKIE, createSessionCookie(session.token()).toString());
		return ResponseEntity.ok(analysisService.analyze(session.sessionId(), ownerName, requests));
	}

	@Operation(
			summary = "분석 결과 조회",
			description = "현재 브라우저의 익명 세션에 속한 분석 결과만 조회합니다. "
					+ "Swagger에서는 먼저 이미지 분석 API를 실행하면 세션 쿠키가 자동으로 사용됩니다.")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "조회 성공",
					content = @Content(schema = @Schema(implementation = AnalysisResponse.class))),
			@ApiResponse(responseCode = "404", description = "세션 또는 분석 결과를 찾을 수 없음", content = @Content)
	})
	@GetMapping(value = "/{analysisId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public AnalysisResponse get(
			@Parameter(
					description = "분석 ID",
					required = true,
					in = ParameterIn.PATH,
					example = "32b6c6d9-700f-445d-b299-6f08a038d2ee")
			@PathVariable UUID analysisId,
			@Parameter(hidden = true)
			@CookieValue(name = SESSION_COOKIE, required = false) String sessionToken) {
		UUID sessionId = sessionService.requireActiveSession(sessionToken);
		return analysisService.get(analysisId, sessionId);
	}

	private ResponseCookie createSessionCookie(String token) {
		return ResponseCookie.from(sessionProperties.cookieName(), token)
				.httpOnly(true)
				.secure(sessionProperties.secure())
				.sameSite("Lax")
				.path("/")
				.maxAge(sessionProperties.maxAge())
				.build();
	}
}
