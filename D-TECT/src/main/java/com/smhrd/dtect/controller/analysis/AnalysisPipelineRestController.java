package com.smhrd.dtect.controller.analysis;

import com.smhrd.dtect.dto.analysis.AnalysisStartResponse;
import com.smhrd.dtect.dto.analysis.AnalysisStatusDto;
import com.smhrd.dtect.entity.analysis.Analysis;
import com.smhrd.dtect.repository.analysis.AnalysisRepository;
import com.smhrd.dtect.service.analysis.AnalysisResultService;
import com.smhrd.dtect.service.pdf.PdfReportOrchestrator;
//import com.smhrd.dtect.service.pdf.PdfWebhookClient;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisPipelineRestController {

    private final AnalysisResultService analysisResultService;
//    private final PdfWebhookClient pdfWebhookClient;
    private final AnalysisRepository analysisRepository;
    private final PdfReportOrchestrator pdfOrchestrator;

    @PostMapping(
            value = "/start",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AnalysisStartResponse> startJson(@RequestBody StartRequest req) {
        if (req == null || req.getUserId() == null) {
            return ResponseEntity.badRequest().build();
        }

        String sid;
        try {
            Method m = analysisResultService.getClass()
                    .getMethod("beginSession", Long.class, Long.class);
            sid = (String) m.invoke(analysisResultService, req.getUserId(), req.getAnalId());
        } catch (NoSuchMethodException nsme) {
            sid = analysisResultService.beginSession(req.getUserId(), (List<MultipartFile>) null);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "start failed", e);
        }

        Long analId = analysisResultService.getAnalIdForSid(sid);
        Instant startedAt = analysisResultService.getStartedAt(sid);

        log.info("[AnalysisStart(JSON)] userId={}, analId={}, sid={}", req.getUserId(), analId, sid);
        return ResponseEntity.ok(new AnalysisStartResponse(sid, analId, startedAt));
    }

    @PostMapping(
            value = "/start",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AnalysisStartResponse> start(
            @RequestParam("userId") Long userId,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        String sid = analysisResultService.beginSession(userId, files);

        Long analId = analysisResultService.getAnalIdForSid(sid);
        Instant startedAt = analysisResultService.getStartedAt(sid);

        return ResponseEntity.ok(new AnalysisStartResponse(sid, analId, startedAt));
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisStatusDto> status(@RequestParam("sid") String sid) {
        return ResponseEntity.ok(analysisResultService.getStatus(sid));
    }

//    @PostMapping(value = "/finalize", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<AnalysisFinalizeResponse> finalizeAnalysis(@RequestParam("sid") String sid) {
//        AnalRate rate = null;
//        int total = 0;
//        Boolean ok = null;
//
//        try {
//            Class<?> cls = analysisResultService.getClass();
//
//            try {
//                Method mark = cls.getMethod("markEnded", String.class);
//                mark.invoke(analysisResultService, sid);
//            } catch (NoSuchMethodException ignored) {}
//
//            Map<?, Integer> counts = null;
//            try {
//                Method getCounts = cls.getMethod("getTypeCounts", String.class);
//                Object res = getCounts.invoke(analysisResultService, sid);
//                if (res instanceof Map) {
//                    counts = (Map<?, Integer>) res;
//                }
//            } catch (NoSuchMethodException ignored) {}
//
//            if (counts != null) {
//                for (Integer v : counts.values()) total += (v != null ? v : 0);
//
//                try {
//                    Method grade = cls.getMethod("gradeByCounts", Map.class);
//                    Object r = grade.invoke(analysisResultService, counts);
//                    if (r instanceof AnalRate) rate = (AnalRate) r;
//                } catch (NoSuchMethodException ignored) {}
//
//                try {
//                    Method fin = cls.getMethod("finalizeNow", String.class);
//                    Object r = fin.invoke(analysisResultService, sid);
//                    if (r instanceof Boolean) ok = (Boolean) r;
//                } catch (NoSuchMethodException ignored) {}
//            }
//        } catch (Exception e) {
//        }
//
//        if (rate == null || ok == null || !ok) {
//            Long userId = analysisResultService.getUserIdForSid(sid);
//            if (userId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown sid");
//
//            List<ModelMessage> items = analysisResultService.getResult(sid);
//            rate = (rate != null ? rate : AnalysisGrader.grade(items));
//            total = (items != null ? items.size() : 0);
//            ok = pdfWebhookClient.dispatchJson(userId, sid, items, rate, null, null);
//        }
//
//        return ResponseEntity.ok(new AnalysisFinalizeResponse(sid, total, rate, ok));
//    }

    @PostMapping(value = "/{analId}/finish", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> finishByAnalId(@PathVariable Long analId) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId));

        Instant now = Instant.now();
        a.setFinishedAt(java.sql.Timestamp.from(now));
        analysisRepository.save(a);

        boolean generated = pdfOrchestrator.generateAndStoreToS3(analId);

        Map<String, Object> body = new HashMap<>();
        body.put("analId", analId);
        body.put("finishedAt", now.toString());
        body.put("generated", generated);
        body.put("reportUrl", generated ? ("/api/analysis/" + analId + "/report/file") : null);
        return ResponseEntity.ok(body);
    }

//
//    @PostMapping(value = "/{analId}/dispatch", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<Map<String, Object>> dispatchByAnalId(@PathVariable Long analId) {
//        boolean ok = analysisResultService.finalizeByAnalId(analId);
//        return ResponseEntity.ok(Map.of("analId", analId, "dispatched", ok));
//    }

    // 리포트 URL 조회 (presigned URL은 다른 컨트롤러에서 생성해도 OK)
//    @GetMapping(value = "/{analId}/report", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<Map<String, Object>> report(@PathVariable Long analId) {
//        return analysisRepository.findById(analId)
//                .map(a -> {
//                    Map<String, Object> out = new HashMap<>();
//                    out.put("reportUrl", a.getReportUrl());
//                    return ResponseEntity.ok(out);
//                })
//                .orElseGet(() ->
//                        ResponseEntity.status(HttpStatus.NOT_FOUND)
//                                .body(Map.of("message", "analysis not found: " + analId))
//                );
//    }

    @Data
    public static class StartRequest {
        private Long userId;
        private Long analId;
    }
}
