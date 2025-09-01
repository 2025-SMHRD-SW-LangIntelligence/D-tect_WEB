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

    /**
     * 프런트(tabshot.js)와 계약:
     * 요청:  { "userId": number, "analId"?: number }
     * 응답:  { "sid": string, "analId": number, "startedAt": ISO-8601 }
     */
    @PostMapping(
        value = "/start",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AnalysisStartResponse> start(@RequestBody StartRequest req) {
        if (req.getUserId() == null) {
            return ResponseEntity.badRequest().build();
        }

        // analId가 있으면 해당 분석과 세션 연결, 없으면 새 Analysis 생성
        String sid = analysisResultService.beginSession(req.getUserId(), req.getAnalId());

        // 방금 만든 세션 정보로 응답 구성
        Long analId = analysisResultService.getAnalIdForSid(sid);
        Instant startedAt = analysisResultService.getStartedAt(sid);

        log.info("[AnalysisStart] userId={}, analId={}, sid={}", req.getUserId(), analId, sid);
        return ResponseEntity.ok(new AnalysisStartResponse(sid, analId, startedAt));
    }

    /** 세션 진행 상태 조회 (sid 기반) */
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisStatusDto> status(@RequestParam("sid") String sid) {
        return ResponseEntity.ok(analysisResultService.getStatus(sid));
    }

    /**
     * 종료 처리:
     * - finishedAt 기록
     * - 누적치 기반으로 n8n 웹훅 전송 (PDF 생성 트리거)
     * - 현재 저장된 reportUrl(있으면) 함께 반환
     */
    @PostMapping(value = "/{analId}/finish", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> finishByAnalId(@PathVariable Long analId) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId));

        Instant now = Instant.now();
        a.setFinishedAt(java.sql.Timestamp.from(now));
        analysisRepository.save(a);

        boolean dispatched;
        String reportUrl = null;
        try {
            // 저장된 누적치로 n8n 웹훅 전송 (PDF 생성 트리거)
            dispatched = analysisResultService.finalizeByAnalId(analId);
            reportUrl = analysisRepository.findById(analId).map(Analysis::getReportUrl).orElse(null);
        } catch (Exception e) {
            log.error("[finish] dispatch error analId={}", analId, e);
            dispatched = false; // ★ 반드시 세팅
        }

        // ★ Map.of 는 null value 불가 → HashMap 사용
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("analId", analId);
        body.put("finishedAt", now.toString());
        body.put("dispatched", dispatched); // ★ 항상 포함
        body.put("reportUrl", reportUrl);   // null 허용(폴링용)

        return ResponseEntity.ok(body);
    }

    /**
     * 리포트 URL 조회 (폴링용)
     * - reportUrl이 아직 없어도 200 + { "reportUrl": null } 로 응답 (NPE 방지)
     */
    @GetMapping(value = "/{analId}/report", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> report(@PathVariable Long analId) {
        return analysisRepository.findById(analId)
            .map(a -> {
                Map<String, Object> out = new HashMap<>();
                out.put("reportUrl", a.getReportUrl()); // null 허용
                return ResponseEntity.ok(out);
            })
            .orElseGet(() ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "analysis not found: " + analId))
            );
    }

    /**
     * (옵션) sid 기반 파이프라인 최종화
     * - 누적 카운트 → 등급 산정 → n8n 웹훅 전송
     */
    @PostMapping(value = "/finalize", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisFinalizeResponse> finalizeAnalysis(@RequestParam("sid") String sid) {
        // 세션 종료 마킹
        analysisResultService.markEnded(sid);

        // 누적 카운트 & 등급 산정
        Map<FieldName,Integer> counts = analysisResultService.getTypeCounts(sid);
        AnalRate rate = analysisResultService.gradeByCounts(counts);
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();

        // n8n 웹훅 전송(세션 기반)
        boolean ok = analysisResultService.finalizeNow(sid);

        return ResponseEntity.ok(new AnalysisFinalizeResponse(sid, total, rate, ok));
    }

    /* ====== 요청 DTO ====== */
    @Data
    public static class StartRequest {
        private Long userId;   // 필수
        private Long analId;   // 선택(있으면 기존 분석과 세션 연결)
    }

    /* ====== 응답 DTO (finish) ====== */
    public record FinishResponse(Long analId, Instant finishedAt, boolean dispatched, String reportUrl) {}
}
