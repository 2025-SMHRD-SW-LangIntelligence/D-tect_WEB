package com.smhrd.dtect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.repository.AnalysisRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisPdfCallbackController {

    private final AnalysisRepository analysisRepository;
    private final EntityManager em;
    private final ObjectMapper mapper;

    /**
     * 예시 요청 바디:
     * {
     *   "sid": "uuid...",
     *   "userId": 123,
     *   "reportPath": "ncp://bucket/key/...",
     *   "analResult": [ ... 모델 원본 배열 ... ],  // 배열 그대로
     *   "analRate": "WARNING"
     * }
     */
    @PostMapping("/pdf-callback")
    public ResponseEntity<?> pdfReady(@RequestBody Map<String, Object> payload) throws Exception {
        Long userId = payload.get("userId") != null ? Long.valueOf(payload.get("userId").toString()) : null;
        String reportPath = payload.get("reportPath") != null ? payload.get("reportPath").toString() : null;
        String analRateStr = payload.get("analRate") != null ? payload.get("analRate").toString() : "NORMAL";
        Object analResultObj = payload.get("analResult");

        if (userId == null || reportPath == null || analResultObj == null) {
            return ResponseEntity.badRequest().body("missing fields");
        }

        String analResultJson = (analResultObj instanceof String)
                ? (String) analResultObj
                : mapper.writeValueAsString(analResultObj);

        Analysis a = new Analysis();
        a.setUser(em.getReference(User.class, userId));
        a.setAnalResult(analResultJson);
        a.setAnalRate(AnalRate.valueOf(analRateStr));
        a.setReportPath(reportPath);
        a.setCreatedAt(Timestamp.from(Instant.now()));

        Analysis saved = analysisRepository.save(a);
        return ResponseEntity.ok(Map.of("analId", saved.getAnalIdx()));
    }
}
