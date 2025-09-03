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

    /**
     * [옵션 트리거] 캡처 종료 시 호출 가능.
     * - A안 우선: sid -> analId 복원되면 analId 기반 finalize(/{analId}/finish 로직과 동일한 효과)
     * - 폴백: analId 복원 실패 시, sid 기반 finalizeNow() 실행
     */
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

        // A안: analId가 있으면 analId 기반 마무리 시도
        if (analId != null) {
            try {
                // finalizeByAnalId가 서비스에 구현되어 있다면 A안으로 전송
                ok = analysisResultService.finalizeByAnalId(analId);
                log.info("[SessionEnd] sid={}, analId={}, dispatchedByAnalId={}", sid, analId, ok);
            } catch (NoSuchMethodError | NoClassDefFoundError e) {
                // 만약 finalizeByAnalId가 아직 없는 코드베이스라면 sid 경로로 폴백
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

    /**
     * 폴링: sid -> analId 매핑 조회
     */
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
