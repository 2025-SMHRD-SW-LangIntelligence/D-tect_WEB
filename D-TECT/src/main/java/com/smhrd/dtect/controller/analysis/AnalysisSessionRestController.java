package com.smhrd.dtect.controller.analysis;

import com.smhrd.dtect.service.analysis.AnalysisResultService;
import com.smhrd.dtect.service.pdf.PdfReportOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis/session")
@RequiredArgsConstructor
@Slf4j
public class AnalysisSessionRestController {

    private final AnalysisResultService analysisResultService;
    private final PdfReportOrchestrator pdfOrchestrator;

    @PostMapping("/end")
    public ResponseEntity<Map<String, Object>> end(@RequestParam String sid) {
        if (sid == null || sid.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "message", "sid is required"
            ));
        }

        analysisResultService.markEnded(sid);

        Long analId = analysisResultService.getAnalIdForSid(sid);
        boolean ok;

        // analId 기반
//        if (analId != null) {
//            try {
//                ok = analysisResultService.finalizeByAnalId(analId);
//            } catch (NoSuchMethodError | NoClassDefFoundError e) {
//                ok = analysisResultService.finalizeNow(sid);
//            }
//        } else {
//            // 폴백: 세션만으로 마무리
//            ok = analysisResultService.finalizeNow(sid);
//        }

        if (analId != null) {
            // ✅ 내부 PDF 생성으로 대체
            try {
                ok = pdfOrchestrator.generateAndStoreToS3(analId);
            } catch (Exception e) {
                log.error("[session/end] PDF generate failed for analId={}", analId, e);
                ok = false;
            }
        } else {
            // 분석 ID가 없으면 보고서 생성이 불가 — 기존 레거시 finalize* 경로는 n8n 전용이라 제거
            ok = false;
        }

        return ResponseEntity.ok(Map.of(
                "ok", ok,
                "analId", analId
        ));
    }

    @GetMapping("/anal-id")
    public ResponseEntity<Map<String, Object>> getAnalId(@RequestParam String sid) {
        if (sid == null || sid.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "analId", null,
                    "message", "sid is required"
            ));
        }
        Long analId = analysisResultService.getAnalIdForSid(sid);
        return ResponseEntity.ok(Map.of("analId", analId));
    }
}
