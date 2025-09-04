package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.AnalysisFinalizeResponse;
import com.smhrd.dtect.dto.AnalysisStartResponse;
import com.smhrd.dtect.dto.AnalysisStatusDto;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.FieldName;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.service.AnalysisResultService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisPipelineRestController {

    private final AnalysisResultService analysisResultService;
    private final AnalysisRepository analysisRepository;

    /** 분석 시작 */
    @PostMapping(
        value = "/start",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AnalysisStartResponse> start(@RequestBody StartRequest req) {
        if (req.getUserId() == null) {
            return ResponseEntity.badRequest().build();
        }

        String sid = analysisResultService.beginSession(req.getUserId(), req.getAnalId());
        Long analId = analysisResultService.getAnalIdForSid(sid);
        Instant startedAt = analysisResultService.getStartedAt(sid);

        log.info("[AnalysisStart] userId={}, analId={}, sid={}", req.getUserId(), analId, sid);
        return ResponseEntity.ok(new AnalysisStartResponse(sid, analId, startedAt));
    }

    /** 세션 상태 조회 */
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisStatusDto> status(@RequestParam("sid") String sid) {
        return ResponseEntity.ok(analysisResultService.getStatus(sid));
    }

    /** 종료 처리 */
    @PostMapping(value = "/{analId}/finish", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> finishByAnalId(@PathVariable Long analId) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId));

        Instant now = Instant.now();
        a.setFinishedAt(java.sql.Timestamp.from(now));
        analysisRepository.save(a);

        Map<String, Object> body = new HashMap<>();
        body.put("analId", analId);
        body.put("finishedAt", now.toString());
        body.put("dispatched", false);
        body.put("reportUrl", a.getReportUrl());

        return ResponseEntity.ok(body);
    }

    /**
     * ✅ 파이프라인 최종화 (n8n 웹훅으로 사용자 이름 포함 전송)
     */
    @PostMapping(value = "/finalize", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisFinalizeResponse> finalizeAnalysis(@RequestParam("sid") String sid) {
        // 세션 종료
        analysisResultService.markEnded(sid);

        // 누적 카운트 & 등급 산정
        Map<FieldName,Integer> counts = analysisResultService.getTypeCounts(sid);
        AnalRate rate = analysisResultService.gradeByCounts(counts);
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();

        // ✅ DB에서 사용자 이름 조회
        Long analId = analysisResultService.getAnalIdForSid(sid);
        Analysis analysis = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId));

        String name = (analysis.getUser() != null && analysis.getUser().getMember() != null)
                ? analysis.getUser().getMember().getName()
                : "사용자";

        // ✅ n8n 웹훅 전송 시 name 포함
        boolean ok = analysisResultService.finalizeNow(sid);

        return ResponseEntity.ok(new AnalysisFinalizeResponse(sid, total, rate, ok));
    }

    /* ====== 요청 DTO ====== */
    @Data
    public static class StartRequest {
        private Long userId;
        private Long analId;
    }

    /* ====== 응답 DTO (finish) ====== */
    public record FinishResponse(Long analId, Instant finishedAt, boolean dispatched, String reportUrl) {}
}
