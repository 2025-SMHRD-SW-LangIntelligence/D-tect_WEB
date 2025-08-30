package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.AnalysisFinalizeResponse;
import com.smhrd.dtect.dto.AnalysisStartResponse;
import com.smhrd.dtect.dto.AnalysisStatusDto;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.FieldName;
import com.smhrd.dtect.service.AnalysisResultService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisPipelineRestController {

    private final AnalysisResultService analysisResultService;

    // 프런트(tabshot.js)와 일치: JSON 바디로 userId(필수), analId(선택)
    @PostMapping(
        value = "/start",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AnalysisStartResponse> start(@RequestBody StartRequest req) {
        if (req.getUserId() == null) {
            return ResponseEntity.badRequest().build();
        }

        // analId는 선택값: 미리 발급된 분석번호가 있으면 세션에 연결, 없으면 null로 시작
        String sid = analysisResultService.beginSession(req.getUserId(), req.getAnalId());
        log.info("[AnalysisStart] userId={}, analId={}, sid={}", req.getUserId(), req.getAnalId(), sid);
        return ResponseEntity.ok(new AnalysisStartResponse(sid));
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisStatusDto> status(@RequestParam("sid") String sid) {
        return ResponseEntity.ok(analysisResultService.getStatus(sid));
    }

    @PostMapping(value = "/finalize", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisFinalizeResponse> finalizeAnalysis(@RequestParam("sid") String sid) {
        // 종료 시각 마킹
        analysisResultService.markEnded(sid);

        // 현재 누적 카운트 & 등급 산정
        Map<FieldName,Integer> counts = analysisResultService.getTypeCounts(sid);
        AnalRate rate = analysisResultService.gradeByCounts(counts);
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();

        // n8n 웹훅 전송까지 서비스에 일원화 (username 복원 포함)
        boolean ok = analysisResultService.finalizeNow(sid);

        return ResponseEntity.ok(new AnalysisFinalizeResponse(sid, total, rate, ok));
    }

    @Data
    private static class StartRequest {
        private Long userId;  // 필수
        private Long analId;  // 선택(있으면 세션에 연결)
    }
}
