package com.goodbuy.backend.analysis.api;

import com.goodbuy.backend.analysis.service.AnalysisApplicationService;
import com.goodbuy.backend.session.AnonymousSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "거래 검토", description = "확인 필요 거래를 사용자가 최종 분류하는 API")
public class TransactionController {

	private static final String SESSION_COOKIE = "goodbuy_session";

	private final AnonymousSessionService sessionService;
	private final AnalysisApplicationService analysisService;

	public TransactionController(
			AnonymousSessionService sessionService,
			AnalysisApplicationService analysisService) {
		this.sessionService = sessionService;
		this.analysisService = analysisService;
	}

	@Operation(
			summary = "거래 분류 확정",
			description = "확인 필요 거래를 소비, 자가 이체 또는 다른 사람 거래로 확정하고 전체 합계를 다시 계산합니다.")
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "거래 수정 및 합계 재계산 성공",
					content = @Content(schema = @Schema(implementation = AnalysisResponse.class))),
			@ApiResponse(responseCode = "400", description = "잘못된 거래 유형 또는 본인 부담액", content = @Content),
			@ApiResponse(responseCode = "404", description = "세션 또는 거래를 찾을 수 없음", content = @Content)
	})
	@PatchMapping(
			value = "/{transactionId}",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public AnalysisResponse review(
			@Parameter(
					description = "수정할 거래 ID",
					required = true,
					in = ParameterIn.PATH,
					example = "ca8be668-63d4-4ae2-a961-33879d3c915c")
			@PathVariable UUID transactionId,
			@Parameter(hidden = true)
			@CookieValue(name = SESSION_COOKIE, required = false) String sessionToken,
			@Valid @RequestBody TransactionReviewRequest request) {
		UUID sessionId = sessionService.requireActiveSession(sessionToken);
		return analysisService.review(transactionId, sessionId, request);
	}
}
