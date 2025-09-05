package com.smhrd.dtect.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.smhrd.dtect.service.pdf.PdfReportOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.FieldName;
import com.smhrd.dtect.service.AnalysisResultService;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/capture")
@RequiredArgsConstructor
@RestController
@Slf4j
public class CaptureRestController {

    private final AnalysisResultService analysisResultService;
    private final PdfReportOrchestrator pdfOrchestrator;

    // ---------- START: 폼/쿼리 ----------
    @PostMapping(path = "/start")
    public ResponseEntity<StartResp> startForm(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "analId", required = false) Long analId
    ) {
        return ResponseEntity.ok(startCommon(userId, analId));
    }

    // ---------- START: JSON ----------
    @PostMapping(path = "/start", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StartResp> startJson(@RequestBody StartReq req) {
        if (req == null || req.userId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return ResponseEntity.ok(startCommon(req.userId(), req.analId()));
    }

    private StartResp startCommon(Long userId, Long analId) {
        String sid = analysisResultService.beginSession(userId, analId);
        Instant startedAt = analysisResultService.getStartedAt(sid);
        return new StartResp(sid, startedAt);
    }

    // ---------- STOP: 폼/쿼리 ----------
    @PostMapping(path = "/stop")
    public ResponseEntity<StopResp> stopForm(@RequestParam("sid") String sid) {
        return ResponseEntity.ok(stopCommon(sid));
    }

    // ---------- STOP: JSON ----------
    @PostMapping(path = "/stop", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StopResp> stopJson(@RequestBody StopReq req) {
        if (req == null || req.sid() == null || req.sid().isBlank()) {
            throw new IllegalArgumentException("sid is required");
        }
        return ResponseEntity.ok(stopCommon(req.sid()));
    }

    private StopResp stopCommon(String sid) {
        analysisResultService.markEnded(sid);

        Map<FieldName, Integer> counts = new LinkedHashMap<>(analysisResultService.getTypeCounts(sid));
        AnalRate rate = analysisResultService.gradeByCounts(counts);

//        boolean dispatched = analysisResultService.finalizeNow(sid); // n8n에 username/횟수 전송
        boolean dispatched = false;
        Long analId = analysisResultService.getAnalIdForSid(sid);
        if (analId != null) {
            try {
                dispatched = pdfOrchestrator.generateAndStoreToS3(analId);
            } catch (Exception e) {
                log.error("[/api/capture/stop] PDF generate failed for sid={}, analId={}", sid, analId, e);
                dispatched = false;
            }
        } else {
            log.warn("[/api/capture/stop] analId not found for sid={}", sid);
        }

        return new StopResp(sid, dispatched, rate, counts);
    }

    public record StartReq(Long userId, Long analId) {}
    public record StopReq(String sid) {}

    public record StartResp(String sid, Instant startedAt) {}
    public record StopResp(String sid, boolean dispatched, AnalRate analRate,
                           Map<FieldName,Integer> typeCounts) {}
}
