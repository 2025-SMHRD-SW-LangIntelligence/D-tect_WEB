package com.smhrd.dtect.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smhrd.dtect.dto.ModelResponse;
import com.smhrd.dtect.service.AnalysisResultService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 모델서버가 결과를 push로 알려줄 때 받는 콜백 엔드포인트.
 * - 모델서버는 배열(JSON array)로 전송한다고 가정 → List<ModelResponse>로 수신
 * - 내부 저장은 ModelMessage로 일원화되므로 Service에서 변환
 */
@RestController
@RequestMapping("/api/analysis/callback")
@RequiredArgsConstructor
@Slf4j
public class AnalysisCallbackController {

    private final AnalysisResultService analysisResultService;

    @Value("${model.callback.secret:}")
    private String callbackSecret; // (선택) HMAC 검증 등에 사용

    @PostMapping(value = "/model", consumes = "application/json")
    public ResponseEntity<Void> onModelCallback(
            @RequestParam("sid") String sid,
            @RequestHeader(value = "X-Model-Signature", required = false) String signature,
            @RequestBody List<ModelResponse> results
    ) {
        log.info("[ModelCallback] sid={}, resultCount={}, signaturePresent={}",
                sid, results != null ? results.size() : 0, signature != null);

        if (sid == null || sid.isBlank()) return ResponseEntity.badRequest().build();
        if (results == null || results.isEmpty()) return ResponseEntity.ok().build();

        // (선택) HMAC 검증 로직 필요 시 여기에 추가

        // Service에서 ModelResponse → ModelMessage로 변환하여 누적
        analysisResultService.appendResponses(sid, results);

        return ResponseEntity.ok().build();
    }
}
