package com.smhrd.dtect.controller;

import com.smhrd.dtect.service.AnalysisResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/analysis/session")
@RequiredArgsConstructor
public class AnalysisSessionRestController {

    private final AnalysisResultService analysisResultService;

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
        if (analId != null) {
            try {
                ok = analysisResultService.finalizeByAnalId(analId);
                log.info("[SessionEnd] sid={}, analId={}, dispatchedByAnalId={}", sid, analId, ok);
            } catch (NoSuchMethodError | NoClassDefFoundError e) {
                log.warn("[SessionEnd] finalizeByAnalId not available, fallback to finalizeNow(sid).");
                ok = analysisResultService.finalizeNow(sid);
            }
        } else {
            // 폴백: 세션만으로 마무리
            ok = analysisResultService.finalizeNow(sid);
            log.info("[SessionEnd] sid={}, analId=null, dispatchedBySid={}", sid, ok);
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
