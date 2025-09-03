package com.smhrd.dtect.controller;

import com.smhrd.dtect.service.AnalysisResultService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisDispatchController {

    private final AnalysisResultService analysisResultService;

    /** 사용자가 결과 보기를 눌렀을 때 PDF 워크플로우 시작 */
    @PostMapping("/{analId}/dispatch")
    public ResponseEntity<?> dispatch(@PathVariable Long analId) {
        boolean ok = analysisResultService.finalizeByAnalId(analId);
        if (ok) {
            return ResponseEntity.ok().body(Map.of("message", "PDF 작업 시작됨"));
        } else {
            return ResponseEntity.status(500).body(Map.of("message", "PDF 작업 시작 실패"));
        }
    }
}
