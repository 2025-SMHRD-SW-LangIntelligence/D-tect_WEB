package com.smhrd.dtect.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.dtect.dto.ModelResultDto;
import com.smhrd.dtect.service.AnalysisResultService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisCallbackController {

    private final AnalysisResultService analysisResultService;

    // 모델과 합의한 비밀키 (없으면 빈 문자열)
    @Value("${app.model.callback-secret:}")
    private String callbackSecret;

    // 모델 서버가 아래 URL로 결과를 POST해줌
    // 예: POST /api/analysis/callback?sid=...   Body: [ {user,text,score,classification{...}}, ... ]
    @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> callback(
            @RequestParam("sid") String sid,
            @RequestHeader(value = "X-Model-Signature", required = false) String signature,
            @RequestBody List<ModelResultDto> results
    ) {
        // (선택) HMAC 검증 ─ 모델과 합의된 방식으로 구현
        if (!callbackSecret.isBlank()) {
            // pseudo: if (!verifyHmac(signature, rawBody, callbackSecret)) return ResponseEntity.status(401).build();
        }

        // 세션에 결과 누적 (push용 메서드 하나 추가)
        analysisResultService.appendResults(sid, results); // ← push 모드용 간단 메서드 추가

        return ResponseEntity.ok().build();
    }
}
