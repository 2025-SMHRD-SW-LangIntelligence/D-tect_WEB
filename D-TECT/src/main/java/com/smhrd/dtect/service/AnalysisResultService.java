package com.smhrd.dtect.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smhrd.dtect.config.PdfProperties;
import com.smhrd.dtect.dto.AnalysisStatusDto;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.FieldName;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.repository.UserRepository;
import com.smhrd.dtect.service.pdf.PdfWebhookClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AnalysisResultService {

    private final PdfWebhookClient pdfWebhookClient;
    private final UserRepository userRepository;
    private final AnalysisRepository analysisRepository;
    private final ObjectMapper om;
    private final PdfProperties pdfProps;

    @Value("${app.analysis.session-ttl-minutes:30}")
    private long ttlMinutes;

    static class SessionState {
        Long ownerUserId;
        Long analId;
        final EnumMap<FieldName, Integer> counts = new EnumMap<>(FieldName.class);
        volatile long received = 0;
        volatile Long total = null;
        volatile Instant startedAt = null;
        volatile Instant endedAt   = null;
        volatile long lastUpdated  = System.currentTimeMillis();
    }

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();
    private final Map<Long, String> analIdToSid = new ConcurrentHashMap<>();

    // ===== 세션 시작 (기존 유지) =====
    public String beginSession(Long userId, List<MultipartFile> files) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));

        Analysis a = new Analysis();
        a.setUser(u);
        a.setCreatedAt(Timestamp.from(Instant.now()));
        a = analysisRepository.save(a);

        String sid = UUID.randomUUID().toString().replace("-", "");
        SessionState st = new SessionState();
        st.ownerUserId = userId;
        st.analId      = a.getAnalIdx();
        st.startedAt   = Instant.now();
        st.lastUpdated = System.currentTimeMillis();
        sessions.put(sid, st);
        analIdToSid.put(st.analId, sid);
        return sid;
    }

    public String beginSession(Long userId, Long analId) {
        Long resolvedAnalId = analId;
        if (resolvedAnalId == null) {
            User u = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
            Analysis a = new Analysis();
            a.setUser(u);
            a.setCreatedAt(Timestamp.from(Instant.now()));
            a = analysisRepository.save(a);
            resolvedAnalId = a.getAnalIdx();
        } else if (!analysisRepository.existsById(resolvedAnalId)) {
            throw new IllegalArgumentException("analysis not found: " + resolvedAnalId);
        }

        String sid = UUID.randomUUID().toString().replace("-", "");
        SessionState st = new SessionState();
        st.ownerUserId = userId;
        st.analId      = resolvedAnalId;
        st.startedAt   = Instant.now();
        st.lastUpdated = System.currentTimeMillis();
        sessions.put(sid, st);
        analIdToSid.put(st.analId, sid);
        return sid;
    }

    // ===== 🔧 누락 메서드 복구/헬퍼 추가 =====
    /** sid -> analId */
    public Long getAnalIdForSid(String sid) {
        SessionState st = sessions.get(sid);
        return (st != null) ? st.analId : null;
    }

    /** analId -> sid */
    public String getSidForAnalId(Long analId) {
        return analIdToSid.get(analId);
    }

    /** analId → username (아이디) */
    public String getUsernameForAnalId(Long analId) {
        return analysisRepository.findById(analId)
                .map(Analysis::getUser)
                .map(User::getUserIdx)
                .flatMap(userRepository::findMemberUsernameByUserId)
                .orElse("");
    }

    /** analId → name (표시명/실명) */
    public String getNameForAnalId(Long analId) {
        return analysisRepository.findById(analId)
                .map(Analysis::getUser)
                .map(u -> u.getMember())
                .map(m -> m.getName())
                .orElse("사용자");
    }

    /** sid → name */
    public String getNameForSid(String sid) {
        Long analId = getAnalIdForSid(sid);
        return (analId != null) ? getNameForAnalId(analId) : "사용자";
    }

    /** sid → username */
    public String getUsernameForSid(String sid) {
        Long analId = getAnalIdForSid(sid);
        return (analId != null) ? getUsernameForAnalId(analId) : "";
    }

    // ===== 상태/집계 (기존 유지) =====
    public long appendTypes(String sid, List<FieldName> types, Long total) {
        if (sid == null || sid.isBlank()) throw new IllegalArgumentException("sid 누락");
        SessionState st = sessions.computeIfAbsent(sid, k -> new SessionState());
        if (types != null) {
            for (FieldName t : types) {
                if (t == null) continue;
                st.counts.merge(t, 1, Integer::sum);
                st.received++;
            }
        }
        if (total != null) st.total = total;
        st.lastUpdated = System.currentTimeMillis();
        return st.counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void markEnded(String sid) {
        SessionState st = sessions.get(sid);
        if (st != null) {
            st.endedAt = Instant.now();
            st.lastUpdated = System.currentTimeMillis();
        }
    }

    public AnalysisStatusDto getStatus(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return new AnalysisStatusDto(0, 0, null);
        long processed = st.counts.values().stream().mapToInt(Integer::intValue).sum();
        return new AnalysisStatusDto(st.received, processed, st.total);
    }

    public Map<FieldName, Integer> getTypeCounts(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return Map.of();
        EnumMap<FieldName, Integer> copy = new EnumMap<>(FieldName.class);
        copy.putAll(st.counts);
        return Collections.unmodifiableMap(copy);
    }

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

    public Instant getStartedAt(String sid) {
        SessionState st = sessions.get(sid);
        return (st != null) ? st.startedAt : null;
    }

    public Instant getEndedAt(String sid) {
        SessionState st = sessions.get(sid);
        return (st != null) ? st.endedAt : null;
    }

    public void clear(String sid) { sessions.remove(sid); }

    @Scheduled(fixedDelay = 60_000)
    void cleanup() {
        long now = System.currentTimeMillis();
        long ttlMs = Math.max(1, ttlMinutes) * 60_000L;
        sessions.entrySet().removeIf(e -> now - e.getValue().lastUpdated > ttlMs);
    }

    // ===== 콜백 저장 (기존 유지) =====
    public void saveFromCallback(Long analId, List<Map<String, Object>> results) {
        if (analId == null) throw new IllegalArgumentException("analId is null");
        if (results == null || results.isEmpty()) return;

        EnumMap<FieldName, Integer> addCounts = new EnumMap<>(FieldName.class);
        long addReceived = 0;
        for (Map<String, Object> item : results) {
            if (item == null) continue;
            List<FieldName> types = extractTypes(item);
            if (types.isEmpty()) continue;
            for (FieldName t : types) {
                if (t == null) continue;
                addCounts.merge(t, 1, Integer::sum);
                addReceived++;
            }
        }

        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("analysis not found: " + analId));

        Map<FieldName, Integer> oldCounts = parseCountsFromAnalResult(a.getAnalResult());
        long oldReceived = parseReceivedFromAnalResult(a.getAnalResult());

        EnumMap<FieldName, Integer> merged = new EnumMap<>(FieldName.class);
        for (FieldName f : FieldName.values()) {
            int base = oldCounts.getOrDefault(f, 0);
            int inc  = addCounts.getOrDefault(f, 0);
            if (base + inc > 0) merged.put(f, base + inc);
        }
        long newReceived = Math.max(0, oldReceived) + Math.max(0, addReceived);

        AnalRate rate = gradeByCounts(merged);
        try {
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

    // ===== 파이널라이즈 (여기서 username+name 둘 다, 그리고 sid 포함) =====
    public boolean finalizeByAnalId(Long analId) {
        Analysis a = analysisRepository.findById(analId)
            .orElseThrow(() -> new IllegalArgumentException("analysis not found: " + analId));

        Map<FieldName,Integer> counts = parseCountsFromAnalResult(a.getAnalResult());
        AnalRate rate = gradeByCounts(counts);

        String username = getUsernameForAnalId(analId); // 아이디
        String name     = getNameForAnalId(analId);     // 표시명/실명

        Instant started = a.getCreatedAt()!=null ? a.getCreatedAt().toInstant() : null;
        Instant ended   = a.getFinishedAt()!=null ? a.getFinishedAt().toInstant(): null;

        String sid = getSidForAnalId(analId); // 있을 수도 없을 수도

        return pdfWebhookClient.dispatchCountsWithAnalId(
                analId, username, name, sid, counts, rate, started, ended
        );
    }

    public boolean finalizeNow(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return false;

        Map<FieldName,Integer> counts = getTypeCounts(sid);
        AnalRate rate = gradeByCounts(counts);

        String username = getUsernameForSid(sid);
        String name     = getNameForSid(sid);

        boolean ok = pdfWebhookClient.dispatchCountsWithAnalId(
                st.analId, username, name, sid, counts, rate, st.startedAt, st.endedAt
        );

        if (st.analId != null) persistMergeToAnalysis(st.analId, counts, st.received);
        return ok;
    }

    // ===== 내부 유틸 (기존 유지) =====
    private Map<FieldName, Integer> parseCountsFromAnalResult(String json) {
        try {
            if (json == null || json.isBlank()) return Map.of();
            var root = om.readTree(json);
            var node = root.hasNonNull("results") ? root.get("results") : root;

            var countsNode = node.get("counts");
            if (countsNode == null || countsNode.isNull()) countsNode = node.get("typeCounts");
            if (countsNode != null && countsNode.isObject()) {
                Map<String, Integer> strMap = om.convertValue(countsNode, new TypeReference<Map<String, Integer>>() {});
                EnumMap<FieldName, Integer> out = new EnumMap<>(FieldName.class);
                for (var e : strMap.entrySet()) {
                    FieldName fn = toFieldName(e.getKey());
                    if (fn != null) out.put(fn, Math.max(0, e.getValue() == null ? 0 : e.getValue()));
                }
                return out;
            }

            if (node.isArray()) {
                EnumMap<FieldName, Integer> out = new EnumMap<>(FieldName.class);
                for (var it : node) {
                    for (FieldName fn : extractTypes(om.convertValue(it, new TypeReference<Map<String, Object>>() {}))) {
                        out.merge(fn, 1, Integer::sum);
                    }
                }
                return out;
            }
        } catch (Exception ignore) {}
        return Map.of();
    }

    private long parseReceivedFromAnalResult(String json) {
        try {
            if (json == null || json.isBlank()) return 0;
            var root = om.readTree(json);
            var node = root.hasNonNull("results") ? root.get("results") : root;
            var v = node.get("received");
            if (v != null && v.isNumber()) return v.longValue();
            if (v != null && v.isTextual()) return Long.parseLong(v.asText().trim());
        } catch (Exception ignore) {}
        return 0;
    }

    private static Map<String, Integer> toStringKeyMap(Map<FieldName, Integer> m) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (var e : m.entrySet()) out.put(e.getKey().name(), e.getValue());
        return out;
    }

    private void persistMergeToAnalysis(Long analId, Map<FieldName, Integer> addCounts, long addReceived) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("analysis not found: " + analId));

        Map<FieldName, Integer> oldCounts = parseCountsFromAnalResult(a.getAnalResult());
        EnumMap<FieldName, Integer> merged = new EnumMap<>(FieldName.class);

        for (FieldName f : FieldName.values()) {
            int base = oldCounts.getOrDefault(f, 0);
            int inc  = addCounts.getOrDefault(f, 0);
            if (base + inc > 0) merged.put(f, base + inc);
        }
        long newReceived = parseReceivedFromAnalResult(a.getAnalResult()) + Math.max(0, addReceived);

        AnalRate rate = gradeByCounts(merged);
        try {
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

    @SuppressWarnings("unchecked")
    private static List<FieldName> extractTypes(Map<String, Object> item) {
        List<FieldName> out = new ArrayList<>();

        Object labels = item.get("labels");
        if (labels instanceof List<?>) {
            addFromList(out, (List<?>) labels);
        }

        Object classification = item.get("classification");
        if (classification instanceof List<?>) {
            List<?> list = (List<?>) classification;
            for (Object o : list) {
                if (o instanceof String s) {
                    FieldName fn = toFieldName(s);
                    if (fn != null) out.add(fn);
                } else if (o instanceof Map<?, ?> m) {
                    Object v = m.get("label");
                    if (v == null) v = m.get("name");
                    if (v == null) v = m.get("type");
                    if (v instanceof String s) {
                        FieldName fn = toFieldName(s);
                        if (fn != null) out.add(fn);
                    }
                }
            }
        }

        for (String k : List.of("label", "type", "category")) {
            Object v = item.get(k);
            if (v instanceof String s) {
                FieldName fn = toFieldName(s);
                if (fn != null) out.add(fn);
            }
        }

        return out;
    }

    private static void addFromList(List<FieldName> out, List<?> list) {
        for (Object v : list) {
            if (v instanceof String s) {
                FieldName fn = toFieldName(s);
                if (fn != null) out.add(fn);
            } else if (v instanceof Map<?, ?> m) {
                Object name = m.get("label");
                if (name == null) name = m.get("name");
                if (name == null) name = m.get("type");
                if (name instanceof String s2) {
                    FieldName fn = toFieldName(s2);
                    if (fn != null) out.add(fn);
                }
            }
        }
    }

    private static FieldName toFieldName(String raw) {
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
}
