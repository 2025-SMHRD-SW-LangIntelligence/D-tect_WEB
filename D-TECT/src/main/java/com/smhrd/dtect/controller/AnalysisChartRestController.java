package com.smhrd.dtect.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smhrd.dtect.dto.ChartDataDto;
import com.smhrd.dtect.repository.AnalysisRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisChartRestController {
    private final AnalysisRepository analysisRepository;
    private final ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();

    @GetMapping("/latest")
    public ChartDataDto latest(@RequestParam Long userId) throws Exception {
        var a = analysisRepository.findTopByUser_UserIdxOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new IllegalArgumentException("최근 분석이 없습니다."));

        // anal_result(JSON 문자열)에서 typeCounts를 꺼내서 counts로 사용
        var root = om.readTree(java.util.Optional.ofNullable(a.getAnalResult()).orElse("{}"));
        var node = root.has("results") ? root.get("results") : root;
        var tc   = node.path("typeCounts");

        java.util.function.Function<String,Integer> get = k -> tc.has(k) ? tc.get(k).asInt(0) : 0;

        var labels = java.util.List.of("폭력","명예훼손","성범죄","따돌림/집단따돌림","협박/강요","공갈/강요");
        var values = java.util.List.of(
                get.apply("VIOLENCE"),
                get.apply("DEFAMATION"),
                get.apply("SEXUAL"),
                get.apply("BULLYING"),
                get.apply("CHANTAGE"),
                get.apply("EXTORTION")
        );
        return new ChartDataDto("최근 분석 결과", labels, values);
    }
}
