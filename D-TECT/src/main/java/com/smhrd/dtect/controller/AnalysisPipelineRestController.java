package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.AnalysisFinalizeResponse;
import com.smhrd.dtect.dto.AnalysisStartResponse;
import com.smhrd.dtect.dto.AnalysisStatusDto;
import com.smhrd.dtect.dto.ModelMessage;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.service.AnalysisGrader;
import com.smhrd.dtect.service.AnalysisResultService;
import com.smhrd.dtect.service.pdf.PdfWebhookClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisPipelineRestController {

    private final AnalysisResultService analysisResultService;
    private final PdfWebhookClient pdfWebhookClient;
    private final AnalysisRepository analysisRepository;

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

    @PostMapping(value = "/finalize", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisFinalizeResponse> finalizeAnalysis(@RequestParam("sid") String sid) {
        Long userId = analysisResultService.getUserIdForSid(sid);
        if (userId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown sid");

        List<ModelMessage> items = analysisResultService.getResult(sid);
        AnalRate rate = AnalysisGrader.grade(items);
        boolean ok = pdfWebhookClient.dispatchJson(userId, sid, items, rate, null, null);

        return ResponseEntity.ok(new AnalysisFinalizeResponse(
                sid, (items != null ? items.size() : 0), rate, ok));
    }

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

    @GetMapping(value = "/{analId}/report", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> report(@PathVariable Long analId) {
        return analysisRepository.findById(analId)
                .map(a -> {
                    Map<String, Object> out = new HashMap<>();
                    out.put("reportUrl", a.getReportUrl());
                    return ResponseEntity.ok(out);
                })
                .orElseGet(() ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("message", "analysis not found: " + analId))
                );
    }

}
