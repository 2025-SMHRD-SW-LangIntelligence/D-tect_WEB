package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.AnalysisFinalizeResponse;
import com.smhrd.dtect.dto.AnalysisStartResponse;
import com.smhrd.dtect.dto.AnalysisStatusDto;
import com.smhrd.dtect.dto.ModelMessage;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.service.AnalysisGrader;
import com.smhrd.dtect.service.AnalysisResultService;
import com.smhrd.dtect.service.pdf.PdfWebhookClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 분석 파이프라인(업로드 → 모델 → 집계 → PDF 웹훅) REST 엔드포인트.
 */
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisPipelineRestController {

    private final AnalysisResultService analysisResultService;
    private final PdfWebhookClient pdfWebhookClient;

    /** 1) 분석 시작: 세션 생성(+ 파일 등록은 Service로 위임) */
    @PostMapping(value = "/start", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisStartResponse> start(
            @RequestParam("userId") Long userId,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        String sid = analysisResultService.beginSession(userId, files);
        return ResponseEntity.ok(new AnalysisStartResponse(sid));
    }

    /** 2) 상태 조회(폴링) */
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisStatusDto> status(@RequestParam("sid") String sid) {
        var status = analysisResultService.getStatus(sid);
        return ResponseEntity.ok(status);
    }

    /** 3) 최종 집계/등급 산정 → PDF 웹훅 전송 */
    @PostMapping(value = "/finalize", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisFinalizeResponse> finalizeAnalysis(
            @RequestParam("sid") String sid,
            @RequestParam("userId") Long userId
    ) {
        List<ModelMessage> items = analysisResultService.getResult(sid);
        AnalRate rate = AnalysisGrader.grade(items);

        boolean ok = pdfWebhookClient.dispatchJson(userId, sid, items, rate, null, null);

        return ResponseEntity.ok(new AnalysisFinalizeResponse(
                sid,
                items != null ? items.size() : 0,
                rate,
                ok
        ));
    }
}
