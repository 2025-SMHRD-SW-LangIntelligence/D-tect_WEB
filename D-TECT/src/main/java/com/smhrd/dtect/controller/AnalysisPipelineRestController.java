package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.*;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.FieldName;
import com.smhrd.dtect.service.AnalysisResultService;
import com.smhrd.dtect.service.pdf.PdfWebhookClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisPipelineRestController {

    private final AnalysisResultService analysisResultService;
    private final PdfWebhookClient pdfWebhookClient;

    @PostMapping(
        value = "/start",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AnalysisStartResponse> start(
            @RequestParam("userId") Long userId,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        // ✨ beginSession 내부에서 tb_analysis 레코드 생성 + started_at 저장
        String sid = analysisResultService.beginSession(userId, files);
        return ResponseEntity.ok(new AnalysisStartResponse(sid));
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisStatusDto> status(@RequestParam("sid") String sid) {
        return ResponseEntity.ok(analysisResultService.getStatus(sid));
    }

    @PostMapping(value = "/finalize", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisFinalizeResponse> finalizeAnalysis(@RequestParam("sid") String sid) {
    	String username = analysisResultService.getUsernameForSid(sid);
        if (username == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown sid");

        // ✨ 유형별 횟수만 꺼냄
        var counts = analysisResultService.getTypeCounts(sid); // {VIOLENCE=30, SEXUAL=10, ...}

        // ✨ 등급은 횟수 기반으로만 산정(내부 로직에서 임계값/가중치 적용)
        var rate   = analysisResultService.gradeByCounts(counts);

        // ✨ 종료 처리(ended_at 저장) + n8n 웹훅 전송(“횟수만”)
        analysisResultService.markEnded(sid);
        boolean ok = pdfWebhookClient.dispatchCounts(
                username,
                sid,
                counts,
                rate,
                analysisResultService.getStartedAt(sid),
                analysisResultService.getEndedAt(sid)
        );

        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        return ResponseEntity.ok(new AnalysisFinalizeResponse(sid, total, rate, ok));
    }
}
