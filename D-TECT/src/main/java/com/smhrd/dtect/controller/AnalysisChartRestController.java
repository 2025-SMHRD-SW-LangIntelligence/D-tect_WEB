package com.smhrd.dtect.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smhrd.dtect.dto.ChartDataDto;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisChartRestController {

    private final AnalysisRepository analysisRepository;
    private final ObjectMapper om; // 스프링 기본 ObjectMapper 주입 사용

    // === 신규: analId 기준 요약 ===
    // GET /api/analysis/{analId}/summary
    @GetMapping("/{analId}/summary")
    public ChartDataDto summaryByAnalId(@PathVariable Long analId) throws Exception {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId));
        return buildDtoFromAnalResult("분석 결과 #" + analId, a.getAnalResult());
    }

    // ===== 내부 유틸 =====
    private ChartDataDto buildDtoFromAnalResult(String title, String analResultJson) throws Exception {
        // 안전 파싱
        JsonNode root = om.readTree(Optional.ofNullable(analResultJson).orElse("{}"));
        // results 또는 루트에서 typeCounts 찾기
        JsonNode node = root.has("results") ? root.get("results") : root;
        JsonNode tc   = node.path("typeCounts"); // ❗ null-safe (없으면 빈 객체로 동작)

        // 라벨(표시용) 고정 순서
        List<String> labels = List.of("폭력","명예훼손","성범죄","따돌림/집단따돌림","협박/강요","공갈/강요");

        // 키(원본 JSON의 필드명) 고정 순서
        List<String> keys = List.of("VIOLENCE","DEFAMATION","SEXUAL","BULLYING","CHANTAGE","EXTORTION");

        // 값 추출(없으면 0)
        List<Integer> values = keys.stream()
                .map(k -> readInt(tc, k))
                .toList();

        return new ChartDataDto(title, labels, values);
    }

    private static int readInt(JsonNode obj, String key) {
        if (obj == null) return 0;
        JsonNode v = obj.get(key);
        if (v == null || v.isNull()) return 0;
        if (v.isNumber()) return v.intValue();
        if (v.isTextual()) {
            try { return Integer.parseInt(v.asText().trim()); } catch (Exception ignore) {}
        }
        return 0;
    }
}
