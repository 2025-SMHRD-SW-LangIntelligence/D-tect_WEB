package com.smhrd.dtect.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smhrd.dtect.dto.ChartDataDto;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisChartRestController {

    private final AnalysisRepository analysisRepository;
    private final ObjectMapper om;

    private static final List<String> KEYS = List.of(
            "VIOLENCE", "DEFAMATION", "SEXUAL", "BULLYING", "CHANTAGE", "EXTORTION"
    );

    // 출력 라벨(한글, 화면 표기용)
    private static final List<String> LABELS = List.of(
            "폭력", "명예훼손", "성범죄", "따돌림/집단따돌림", "협박", "공갈"
    );

    @GetMapping("/{analId}/summary")
    public ChartDataDto summaryByAnalId(@PathVariable Long analId) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId));

        String json = a.getAnalResult(); // 예: {"counts":{"VIOLENCE":2,...},"received":10}
        Map<String, Integer> counts = parseCountsSafe(json);

        List<Integer> values = new ArrayList<>(KEYS.size());
        for (String k : KEYS) {
            values.add(counts.getOrDefault(k, 0));
        }

        String title = "분석 결과 #" + analId;
        return new ChartDataDto(title, LABELS, values);
    }

    private Map<String, Integer> parseCountsSafe(String analResultJson) {
        try {
            JsonNode root = om.readTree(Optional.ofNullable(analResultJson).orElse("{}"));

            JsonNode node = root.hasNonNull("results") ? root.get("results") : root;

            JsonNode countsNode = firstNonNullNode(
                    node.get("counts"),
                    node.get("typeCounts"),
                    node.path("type_counts") // 혹시 스네이크케이스 사용 시
            );
            if (isObject(countsNode)) {
                return mapObjectCounts(countsNode);
            }

            if (node.isArray()) {
                return scanArrayCounts(node);
            }

            if (root.isArray()) {
                return scanArrayCounts(root);
            }


            for (String arrKey : List.of("messages", "data")) {
                JsonNode arr = node.get(arrKey);
                if (arr != null && arr.isArray()) {
                    return scanArrayCounts(arr);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse analResult JSON safely: {}", e.toString());
        }
        return Collections.emptyMap();
    }

    private static Map<String, Integer> mapObjectCounts(JsonNode obj) {
        Map<String, Integer> out = new HashMap<>();
        obj.fieldNames().forEachRemaining(fn -> {
            String key = normalizeKey(fn);
            int val = readInt(obj, fn);
            if (key != null) out.merge(key, Math.max(0, val), Integer::sum);
        });
        return out;
    }

    private static Map<String, Integer> scanArrayCounts(JsonNode arr) {
        Map<String, Integer> out = new HashMap<>();
        for (JsonNode e : arr) {
            if (e == null || e.isNull()) continue;

            String single = firstNonNullText(
                    e.get("type"), e.get("caseType"), e.get("label"), e.get("name"), e.get("category")
            );
            addOne(out, single);

            JsonNode labels = e.get("labels");
            if (labels != null && labels.isArray()) {
                for (JsonNode it : labels) {
                    if (it.isTextual()) addOne(out, it.asText());
                    else if (it.isObject()) addOne(out, firstNonNullText(it.get("label"), it.get("name"), it.get("type")));
                }
            }

            JsonNode classification = e.get("classification");
            if (classification != null && classification.isArray()) {
                for (JsonNode it : classification) {
                    if (it.isTextual()) addOne(out, it.asText());
                    else if (it.isObject()) addOne(out, firstNonNullText(it.get("label"), it.get("name"), it.get("type")));
                }
            }
        }
        return out;
    }

    private static void addOne(Map<String, Integer> map, String raw) {
        String key = normalizeKey(raw);
        if (key == null) return;
        map.merge(key, 1, Integer::sum);
    }

    private static String normalizeKey(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        s = s.toUpperCase(Locale.ROOT);

        switch (s) {
            case "HARASSMENT": s = "BULLYING"; break;
            case "BLACKMAIL" : s = "CHANTAGE"; break;
            case "VIOLENT"   : s = "VIOLENCE"; break;
        }

        return KEYS.contains(s) ? s : null;
    }

    private static boolean isObject(JsonNode n) {
        return n != null && n.isObject() && !n.isNull();
    }

    private static JsonNode firstNonNullNode(JsonNode... nodes) {
        for (JsonNode n : nodes) if (n != null && !n.isNull()) return n;
        return null;
    }

    private static String firstNonNullText(JsonNode... nodes) {
        for (JsonNode n : nodes) {
            if (n != null && n.isTextual()) {
                String s = n.asText(null);
                if (s != null && !s.isBlank()) return s;
            }
        }
        return null;
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
