package com.smhrd.dtect.controller;

import com.smhrd.dtect.service.AnalysisResultService;
import com.smhrd.dtect.service.pdf.PdfReportOrchestrator;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisDispatchController {

//    private final AnalysisResultService analysisResultService;
    private final PdfReportOrchestrator pdfOrchestrator;

    // 사용자가 결과 보기를 눌렀을 때 PDF 생성
    @PostMapping("/{analId}/dispatch")
    public ResponseEntity<?> dispatch(@PathVariable Long analId) {
        boolean ok = pdfOrchestrator.generateAndStoreToS3(analId);
        if (ok) {
            return ResponseEntity.ok().body(Map.of("message", "PDF 생성이 시작/완료되었습니다."));
        } else {
            return ResponseEntity.status(500).body(Map.of("message", "PDF 생성 실패"));
        }
    }
}
