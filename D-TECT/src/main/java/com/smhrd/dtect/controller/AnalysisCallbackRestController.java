package com.smhrd.dtect.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smhrd.dtect.entity.FieldName;
import com.smhrd.dtect.service.AnalysisResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 단일 콜백 엔드포인트:
 * - 결과 저장(analId 경로) + 실시간 유형 누적(sid 경로) 모두 처리.
 * - 쿼리 파라미터는 문자열로 받아 "undefined"/"null"/""를 안전하게 무시한 뒤 수동 파싱.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisCallbackRestController {

    private final AnalysisResultService analysisResultService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> callback(
            @RequestParam(value = "analId", required = false) String analIdParam,
            @RequestParam(value = "sid",    required = false) String sid,
            @RequestParam(value = "total",  required = false) String totalParam,
            @RequestBody Object payload,
            @RequestHeader(value = "Content-Type", required = false) String contentType
    ) {
        Long analId = parseLongOrNull(analIdParam);
        Long total  = parseLongOrNull(totalParam);

        // ---- analId 경로: 분석 결과 저장 ----
        if (analId != null) {
            List<Map<String, Object>> results = normalizeToListOfMaps(payload);
            if (results.isEmpty()) {
                log.warn("[Callback] analId={}, contentType={}, empty results", analId, contentType);
                return ResponseEntity.noContent().build();
            }
            log.info("[Callback] analId={}, contentType={}, resultCount={}", analId, contentType, results.size());
            analysisResultService.saveFromCallback(analId, results);
            return ResponseEntity.ok().build();
        }

        // ---- sid 경로: 실시간 유형 누적 ----
        if (sid != null && !sid.isBlank()) {
            List<?> arr = normalizeToList(payload);
            List<FieldName> types = new ArrayList<>();
            for (Object elem : arr) {
                types.addAll(extractTypes(elem));
            }
            if (types.isEmpty()) {
                log.warn("[Callback] sid={}, contentType={}, no types extracted", sid, contentType);
                return ResponseEntity.noContent().build();
            }
            analysisResultService.appendTypes(sid, types, total);
            log.info("[Callback] sid={}, contentType={}, appendedTypes={}, total={}", sid, contentType, types.size(), total);
            return ResponseEntity.ok().build();
        }

        // ---- 둘 다 없음 ----
        log.warn("[Callback] missing analId/sid. raw analIdParam='{}', sid='{}', contentType={}",
                analIdParam, sid, contentType);
        return ResponseEntity.badRequest().build();
    }

    /* ================= Normalizers ================= */

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeToListOfMaps(Object payload) {
        List<?> base = normalizeToList(payload);
        if (base.isEmpty()) return List.of();
        return objectMapper.convertValue(base, new TypeReference<List<Map<String, Object>>>() {});
    }

    /** payload → List (배열, {messages|results|data:[...]}, 단일객체→[obj]) */
    @SuppressWarnings("unchecked")
    private static List<?> normalizeToList(Object payload) {
        if (payload == null) return List.of();

        if (payload instanceof List<?> list) return list;

        if (payload instanceof Map<?, ?> map) {
            Object arr = firstNonNull(map.get("messages"), map.get("results"), map.get("data"));
            if (arr instanceof List<?>) return (List<?>) arr;
            return List.of(map); // 단일 객체
        }
        return List.of(payload); // 원시값 등
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... vals) {
        for (T v : vals) if (v != null) return v;
        return null;
    }

    /* =============== Type extraction (sid 경로) =============== */

    @SuppressWarnings("unchecked")
    private static List<FieldName> extractTypes(Object elem) {
        List<FieldName> out = new ArrayList<>();
        if (elem == null) return out;

        if (elem instanceof String s) {
            FieldName fn = toFieldName(s);
            if (fn != null) out.add(fn);
            return out;
        }

        if (elem instanceof Map<?, ?> m) {
            Object t1 = firstNonNull(m.get("type"), m.get("caseType"), m.get("label"), m.get("name"), m.get("category"));
            if (t1 instanceof String s) {
                FieldName fn = toFieldName(s);
                if (fn != null) out.add(fn);
            }

            Object labels = m.get("labels");
            if (labels instanceof List<?> l) {
                for (Object it : l) {
                    if (it instanceof String s2) {
                        FieldName fn = toFieldName(s2);
                        if (fn != null) out.add(fn);
                    } else if (it instanceof Map<?, ?> lm) {
                        Object lv = firstNonNull(lm.get("label"), lm.get("name"), lm.get("type"));
                        if (lv instanceof String s3) {
                            FieldName fn = toFieldName(s3);
                            if (fn != null) out.add(fn);
                        }
                    }
                }
            }

            Object classification = m.get("classification");
            if (classification instanceof List<?> l2) {
                for (Object it : l2) {
                    if (it instanceof String s4) {
                        FieldName fn = toFieldName(s4);
                        if (fn != null) out.add(fn);
                    } else if (it instanceof Map<?, ?> lm2) {
                        Object lv2 = firstNonNull(lm2.get("label"), lm2.get("name"), lm2.get("type"));
                        if (lv2 instanceof String s5) {
                            FieldName fn = toFieldName(s5);
                            if (fn != null) out.add(fn);
                        }
                    }
                }
            }
        }
        return out;
    }

    /** 문자열 → FieldName (대소문자 무시 + 일부 별칭 보정) */
    private static FieldName toFieldName(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        switch (s) { // 별칭 보정
            case "HARASSMENT" -> s = "BULLYING";
            case "BLACKMAIL"  -> s = "CHANTAGE";
            case "VIOLENT"    -> s = "VIOLENCE";
        }
        try { return FieldName.valueOf(s); }
        catch (Exception ignore) { return null; }
    }

    /* ================= Utilities ================= */

    private static Long parseLongOrNull(String s) {
        if (s == null) return null;
        String t = s.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty() || "undefined".equals(t) || "null".equals(t)) return null;
        try { return Long.valueOf(t); }
        catch (NumberFormatException e) { return null; }
    }
}
