package com.smhrd.dtect.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smhrd.dtect.dto.*;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.Case;
import com.smhrd.dtect.entity.FieldName;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.repository.CaseRepository;
import com.smhrd.dtect.service.pdf.PdfWebhookClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisResultService {

    private final PdfWebhookClient pdfWebhookClient;
    private final AnalysisRepository analysisRepository;
    private final CaseRepository caseRepository;

    @Value("${app.analysis.session-ttl-minutes:30}")
    private long ttlMinutes;

    // ===== 세션 상태 =====
    static class SessionState {
        Long ownerUserId;
        Long analId;

        final List<ModelMessage> items = Collections.synchronizedList(new ArrayList<>());
        final Set<String> fingerprints = ConcurrentHashMap.newKeySet();

        final EnumMap<FieldName, Integer> counts = new EnumMap<>(FieldName.class);

        volatile long received = 0, processed = 0;
        volatile Long total = null;
        volatile long lastUpdated = System.currentTimeMillis();

        volatile Instant startedAt = null;
        volatile Instant endedAt   = null;
    }

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();
    private final Map<Long, String> analIdToSid = new ConcurrentHashMap<>();

    // ===== 세션 시작 =====
    public String beginSession(Long userId, List<MultipartFile> files) {
        String sid = UUID.randomUUID().toString().replace("-", "");
        SessionState st = new SessionState();
        st.ownerUserId = userId;
        st.startedAt   = Instant.now();
        st.lastUpdated = System.currentTimeMillis();
        sessions.put(sid, st);
        return sid;
    }

    public String beginSession(Long userId, Long analId) {
        String sid = UUID.randomUUID().toString().replace("-", "");
        SessionState st = new SessionState();
        st.ownerUserId = userId;
        st.analId      = analId;
        st.startedAt   = Instant.now();
        st.lastUpdated = System.currentTimeMillis();
        sessions.put(sid, st);
        if (analId != null) analIdToSid.put(analId, sid);
        return sid;
    }

    public Long getAnalIdForSid(String sid) {
        SessionState st = sessions.get(sid);
        return (st != null) ? st.analId : null;
    }

    public Long getUserIdForSid(String sid) {
        SessionState st = sessions.get(sid);
        return (st != null) ? st.ownerUserId : null;
    }

    public Instant getStartedAt(String sid) {
        SessionState st = sessions.get(sid);
        return (st != null) ? st.startedAt : null;
    }

    public void markEnded(String sid) {
        SessionState st = sessions.get(sid);
        if (st != null) {
            st.endedAt = Instant.now();
            st.lastUpdated = System.currentTimeMillis();
        }
    }

    // ===== 상태 조회 =====
    public AnalysisStatusDto getStatus(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return new AnalysisStatusDto(0, 0, null);
        long processedNow = st.counts.values().stream().mapToInt(Integer::intValue).sum();
        return new AnalysisStatusDto(st.received, processedNow, st.total);
    }

    public List<ModelMessage> getResult(String sid) {
        SessionState st = sessions.get(sid);
        return (st == null) ? List.of() : copyOf(st.items);
    }

    public void clear(String sid) { sessions.remove(sid); }

    @Scheduled(fixedDelay = 60_000)
    void cleanup() {
        long now = System.currentTimeMillis();
        long ttlMs = Math.max(1, ttlMinutes) * 60_000L;
        sessions.entrySet().removeIf(e -> now - e.getValue().lastUpdated > ttlMs);
    }

    // ===== 모델 라벨값 저장 (/callback-model) =====
    @Transactional
    public void appendResults(Long analId, List<ModelMessage> results, Long total) {
        Analysis analysis = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis not found: " + analId));

        List<Case> toSave = new ArrayList<>();
        for (ModelMessage m : results) {
            if (m.getClassification() == null) continue;
            for (LabelCount lc : m.getClassification()) {
                if (lc == null || lc.getCount() <= 0) continue;
                try {
                    FieldName fn = FieldName.valueOf(
                            (lc.getLabel() == null ? "" : lc.getLabel()).toUpperCase()
                    );
                    Case c = new Case();
                    c.setAnalysis(analysis);
                    c.setCaseType(fn);
                    toSave.add(c);
                } catch (IllegalArgumentException ignore) {}
            }
        }
        if (!toSave.isEmpty()) {
            caseRepository.saveAll(toSave);
            log.info("Saved {} CASE(s) for analId={}", toSave.size(), analId);
        }
    }

    @Transactional
    public void saveFromCallbackMessages(Long analId, List<ModelMessage> results) {
        appendResults(analId, results, null);
    }

    // ===== 라벨 카운트 누적 (/callback, Map payload) =====
    @Transactional
    public void saveFromCallback(Long analId, List<Map<String, Object>> results) {
        if (analId == null) throw new IllegalArgumentException("analId is null");
        if (results == null || results.isEmpty()) return;

        EnumMap<FieldName, Integer> addCounts = new EnumMap<>(FieldName.class);
        long addReceived = 0;

        for (Map<String, Object> item : results) {
            if (item == null) continue;
            List<FieldName> types = extractTypesFromMap(item);
            if (types.isEmpty()) continue;
            for (FieldName t : types) {
                if (t == null) continue;
                addCounts.merge(t, 1, Integer::sum);
            }
            addReceived++;
        }

        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis not found: " + analId));

        Map<FieldName, Integer> oldCounts = parseCountsFromAnalResultJson(a.getAnalResult());
        long oldReceived = parseReceivedFromAnalResultJson(a.getAnalResult());

        EnumMap<FieldName, Integer> merged = new EnumMap<>(FieldName.class);
        for (FieldName f : FieldName.values()) {
            int base = oldCounts.getOrDefault(f, 0);
            int inc  = addCounts.getOrDefault(f, 0);
            if (base + inc > 0) merged.put(f, base + inc);
        }
        long newReceived = Math.max(0, oldReceived) + Math.max(0, addReceived);

        // 등급 재산정 반영
        AnalRate rate = gradeByCounts(merged);

        try {
            ObjectMapper om = new ObjectMapper();
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("counts", toStringKeyMap(merged));
            summary.put("received", newReceived);

            a.setAnalRate(rate);                 // ★ 등급 저장
            a.setAnalResult(om.writeValueAsString(summary));  // 누적 카운트 저장
            analysisRepository.save(a);
        } catch (Exception e) {
            throw new RuntimeException("failed to persist merged analysis result", e);
        }
    }

    // ===== 실시간 유형 누적( sid 경로 ) =====
    public long appendTypes(String sid, List<FieldName> types, Long total) {
        if (sid == null || sid.isBlank()) throw new IllegalArgumentException("sid is required");
        SessionState st = sessions.computeIfAbsent(sid, k -> new SessionState());
        if (types != null) {
            for (FieldName t : types) {
                if (t == null) continue;
                st.counts.merge(t, 1, Integer::sum);
                st.received++;
            }
        }
        if (total != null) st.total = total;
        st.processed = st.counts.values().stream().mapToInt(Integer::intValue).sum();
        st.lastUpdated = System.currentTimeMillis();
        return st.processed;
    }

    // ===== 등급 산정 =====
    public AnalRate gradeByCounts(Map<FieldName, Integer> counts) {
        if (counts == null || counts.isEmpty()) return AnalRate.NORMAL;

        Map<FieldName, Integer> W = Map.of(
                FieldName.VIOLENCE,   1,
                FieldName.DEFAMATION, 1,
                FieldName.SEXUAL,     1,
                FieldName.BULLYING,   1,
                FieldName.CHANTAGE,   1,
                FieldName.EXTORTION,  1
        );

        int weighted = 0;
        for (var e : counts.entrySet()) {
            int w = W.getOrDefault(e.getKey(), 1);
            weighted += w * Math.max(0, e.getValue());
        }
        if (weighted >= 30) return AnalRate.DANGER;
        if (weighted >= 15) return AnalRate.WARNING;
        return AnalRate.NORMAL;
    }

    // ===== 세션 종료 시 웹훅 + DB 누적 보정 =====
    public boolean finalizeNow(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return false;

        Map<FieldName, Integer> counts = getTypeCounts(sid);
        AnalRate rate = gradeByCounts(counts);

        boolean ok = pdfWebhookClient.dispatchCountsWithAnalId(
                st.analId, "", sid, counts, rate, st.startedAt, st.endedAt
        );

        if (st.analId != null) {
            persistMergeToAnalysis(st.analId, counts, st.received);
        }
        return ok;
    }

    public boolean finalizeByAnalId(Long analId) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis not found: " + analId));

        Map<FieldName, Integer> counts = parseCountsFromAnalResultJson(a.getAnalResult());
        AnalRate rate = gradeByCounts(counts);

        String username = "";
        String sid = analIdToSid.get(analId);
        Instant started = (a.getCreatedAt()  != null) ? a.getCreatedAt().toInstant()  : null;
        Instant ended   = (a.getFinishedAt() != null) ? a.getFinishedAt().toInstant() : null;

        try {
            return pdfWebhookClient.dispatchCountsWithAnalId(
                    analId, username, sid, counts, rate, started, ended
            );
        } catch (NoSuchMethodError | RuntimeException e) {
            return pdfWebhookClient.dispatchJson(
                    (a.getUser() != null ? a.getUser().getUserIdx() : null),
                    "anal-" + analId, List.of(), rate, started, ended
            );
        }
    }

    // ===== 파싱/유틸 =====
    @SuppressWarnings("unchecked")
    private static List<FieldName> extractTypesFromMap(Map<String, Object> item) {
        List<FieldName> out = new ArrayList<>();

        Object labels = item.get("labels");
        if (labels instanceof List<?> list) {
            for (Object v : list) {
                if (v instanceof String s) {
                    FieldName fn = toFieldNameFlexible(s);
                    if (fn != null) out.add(fn);
                } else if (v instanceof Map<?, ?> m) {
                    Object name = m.get("label");
                    if (name == null) name = m.get("name");
                    if (name == null) name = m.get("type");
                    if (name instanceof String s2) {
                        FieldName fn = toFieldNameFlexible(s2);
                        if (fn != null) out.add(fn);
                    }
                }
            }
        }

        Object classification = item.get("classification");
        if (classification instanceof List<?> list2) {
            for (Object o : list2) {
                if (o instanceof String s) {
                    FieldName fn = toFieldNameFlexible(s);
                    if (fn != null) out.add(fn);
                } else if (o instanceof Map<?, ?> m) {
                    Object v = m.get("label");
                    if (v == null) v = m.get("name");
                    if (v == null) v = m.get("type");
                    if (v instanceof String s3) {
                        FieldName fn = toFieldNameFlexible(s3);
                        if (fn != null) out.add(fn);
                    }
                }
            }
        }

        for (String k : List.of("label", "type", "category")) {
            Object v = item.get(k);
            if (v instanceof String s) {
                FieldName fn = toFieldNameFlexible(s);
                if (fn != null) out.add(fn);
            }
        }
        return out;
    }

    private static FieldName toFieldNameFlexible(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        switch (s) {
            case "HARASSMENT" -> s = "BULLYING";
            case "BLACKMAIL"  -> s = "CHANTAGE";
            case "VIOLENT"    -> s = "VIOLENCE";
        }
        try { return FieldName.valueOf(s); }
        catch (Exception ignore) { return null; }
    }

    private Map<FieldName, Integer> parseCountsFromAnalResultJson(String json) {
        try {
            if (json == null || json.isBlank()) return Map.of();
            ObjectMapper om = new ObjectMapper();
            var root = om.readTree(json);
            var node = root.hasNonNull("results") ? root.get("results") : root;

            var countsNode = (node != null) ? node.get("counts") : null;
            if (countsNode != null && countsNode.isObject()) {
                Map<String, Integer> strMap =
                        om.convertValue(countsNode, new TypeReference<Map<String, Integer>>() {});
                EnumMap<FieldName, Integer> out = new EnumMap<>(FieldName.class);
                for (var e : strMap.entrySet()) {
                    FieldName fn = toFieldNameFlexible(e.getKey());
                    if (fn != null) out.put(fn, Math.max(0, e.getValue() == null ? 0 : e.getValue()));
                }
                return out;
            }
        } catch (Exception ignore) {}
        return Map.of();
    }

    private long parseReceivedFromAnalResultJson(String json) {
        try {
            if (json == null || json.isBlank()) return 0;
            ObjectMapper om = new ObjectMapper();
            var root = om.readTree(json);
            var node = root.hasNonNull("results") ? root.get("results") : root;
            var v = (node != null) ? node.get("received") : null;
            if (v != null && v.isNumber())  return v.longValue();
            if (v != null && v.isTextual()) return Long.parseLong(v.asText().trim());
        } catch (Exception ignore) {}
        return 0;
    }

    private static Map<String, Integer> toStringKeyMap(Map<FieldName, Integer> m) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (var e : m.entrySet()) out.put(e.getKey().name(), e.getValue());
        return out;
    }

    private static List<ModelMessage> copyOf(List<ModelMessage> list){
        synchronized (list) { return List.copyOf(list); }
    }

    public Map<FieldName, Integer> getTypeCounts(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return Map.of();

        if (st.counts != null && !st.counts.isEmpty()) {
            EnumMap<FieldName, Integer> copy = new EnumMap<>(FieldName.class);
            copy.putAll(st.counts);
            return copy;
        }

        EnumMap<FieldName, Integer> out = new EnumMap<>(FieldName.class);
        synchronized (st.items) {
            for (ModelMessage mm : st.items) {
                FieldName fn = pickTopLabel(mm);
                if (fn != null) out.merge(fn, 1, Integer::sum);
            }
        }
        return out;
    }

    private FieldName pickTopLabel(ModelMessage m) {
        if (m.getClassification() == null || m.getClassification().isEmpty()) return null;
        LabelCount best = m.getClassification().stream()
                .max(Comparator.comparingInt(LabelCount::getCount))
                .orElse(null);
        if (best == null) return null;

        String raw = (best.getLabel() == null) ? "" : best.getLabel().trim().toUpperCase();
        try { return FieldName.valueOf(raw); }
        catch (IllegalArgumentException iae) {
            log.warn("Unknown label from model: '{}'", raw);
            return null;
        }
    }

    // DB에 누적 merge (세션 종료/콜백 공용)
    private void persistMergeToAnalysis(Long analId, Map<FieldName, Integer> addCounts, long addReceived) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis not found: " + analId));

        Map<FieldName, Integer> oldCounts = parseCountsFromAnalResultJson(a.getAnalResult());
        EnumMap<FieldName, Integer> merged = new EnumMap<>(FieldName.class);

        for (FieldName f : FieldName.values()) {
            int base = oldCounts.getOrDefault(f, 0);
            int inc  = addCounts.getOrDefault(f, 0);
            if (base + inc > 0) merged.put(f, base + inc);
        }
        long newReceived = parseReceivedFromAnalResultJson(a.getAnalResult()) + Math.max(0, addReceived);

        AnalRate rate = gradeByCounts(merged);
        try {
            ObjectMapper om = new ObjectMapper();
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("counts", toStringKeyMap(merged));
            summary.put("received", newReceived);
            a.setAnalRate(rate);
            a.setAnalResult(om.writeValueAsString(summary));
            analysisRepository.save(a);
        } catch (Exception e) {
            throw new RuntimeException("failed to persist merged analysis result", e);
        }
    }
}
