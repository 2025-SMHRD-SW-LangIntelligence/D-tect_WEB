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

/**
 * 분석 요약(차트 데이터) 엔드포인트
 * - 다양한 analResult 포맷(counts/typeCounts/배열 등)을 유연하게 파싱하여
 *   고정 라벨 순서에 맞춘 values를 반환.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisChartRestController {

    private final AnalysisRepository analysisRepository;
    private final ObjectMapper om; // 스프링의 전역 ObjectMapper

    // 고정 출력 순서(원본 키)
    private static final List<String> KEYS = List.of(
            "VIOLENCE", "DEFAMATION", "SEXUAL", "BULLYING", "CHANTAGE", "EXTORTION"
    );

    // 출력 라벨(한글, 화면 표기용) — 필요 시 텍스트만 조정
    private static final List<String> LABELS = List.of(
            "폭력", "명예훼손", "성범죄", "따돌림/집단따돌림", "협박", "공갈"
    );

    // GET /api/analysis/{analId}/summary
    @GetMapping("/{analId}/summary")
    public ChartDataDto summaryByAnalId(@PathVariable Long analId) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId));

        String json = a.getAnalResult(); // 예: {"counts":{"VIOLENCE":2,...},"received":10}
        Map<String, Integer> counts = parseCountsSafe(json);

        // 고정 순서대로 values 구성
        List<Integer> values = new ArrayList<>(KEYS.size());
        for (String k : KEYS) {
            values.add(counts.getOrDefault(k, 0));
        }

        String title = "분석 결과 #" + analId;
        return new ChartDataDto(title, LABELS, values);
    }

    /* ================= 파싱 로직 ================= */

    /**
     * analResult JSON을 안전하게 파싱하여 <원본Key -> 수치> 맵을 얻는다.
     * 지원 포맷:
     * - {"counts": {...}}
     * - {"typeCounts": {...}} / {"results": {"typeCounts": {...}}}
     * - 배열/래핑객체: [{"type":"..."}, ...], {"results":[...]}, {"messages":[...]}, {"data":[...]}
     */
    private Map<String, Integer> parseCountsSafe(String analResultJson) {
        try {
            JsonNode root = om.readTree(Optional.ofNullable(analResultJson).orElse("{}"));
            // 1) results 래핑 우선 제거
            JsonNode node = root.hasNonNull("results") ? root.get("results") : root;

            // 2) counts / typeCounts 바로 찾기
            JsonNode countsNode = firstNonNullNode(
                    node.get("counts"),
                    node.get("typeCounts"),
                    node.path("type_counts") // 혹시 스네이크케이스 사용 시
            );
            if (isObject(countsNode)) {
                return mapObjectCounts(countsNode);
            }

            // 3) node가 배열이라면 항목 스캔
            if (node.isArray()) {
                return scanArrayCounts(node);
            }

            // 4) root 자체가 배열이라면 스캔
            if (root.isArray()) {
                return scanArrayCounts(root);
            }

            // 5) results/messages/data 내부 배열 스캔
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

    /** JsonObject 형태의 counts를 맵으로 변환 */
    private static Map<String, Integer> mapObjectCounts(JsonNode obj) {
        Map<String, Integer> out = new HashMap<>();
        obj.fieldNames().forEachRemaining(fn -> {
            String key = normalizeKey(fn);
            int val = readInt(obj, fn);
            if (key != null) out.merge(key, Math.max(0, val), Integer::sum);
        });
        return out;
    }

    /** 배열/래핑객체를 스캔하여 라벨들을 카운팅 */
    private static Map<String, Integer> scanArrayCounts(JsonNode arr) {
        Map<String, Integer> out = new HashMap<>();
        for (JsonNode e : arr) {
            if (e == null || e.isNull()) continue;

            // 1) 단일 키(type/caseType/label/name/category)
            String single = firstNonNullText(
                    e.get("type"), e.get("caseType"), e.get("label"), e.get("name"), e.get("category")
            );
            addOne(out, single);

            // 2) labels: ["VIOLENCE", ...] or [{"label":"..."}, ...]
            JsonNode labels = e.get("labels");
            if (labels != null && labels.isArray()) {
                for (JsonNode it : labels) {
                    if (it.isTextual()) addOne(out, it.asText());
                    else if (it.isObject()) addOne(out, firstNonNullText(it.get("label"), it.get("name"), it.get("type")));
                }
            }

            // 3) classification: ["...", ...] or [{"label":"..."}, ...]
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

    /** 문자열 라벨 → 원본 Key(대문자)로 정규화 + 별칭 보정 */
    private static String normalizeKey(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        s = s.toUpperCase(Locale.ROOT);

        // 별칭/동의어 보정
        switch (s) {
            case "HARASSMENT": s = "BULLYING"; break;
            case "BLACKMAIL" : s = "CHANTAGE"; break;
            case "VIOLENT"   : s = "VIOLENCE"; break;
        }

        // 지원 키만 허용
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
