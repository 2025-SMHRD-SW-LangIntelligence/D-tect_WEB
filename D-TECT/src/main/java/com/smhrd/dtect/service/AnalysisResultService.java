package com.smhrd.dtect.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper; // ★ 추가
import com.smhrd.dtect.config.PdfProperties;
import com.smhrd.dtect.config.PdfWebClientConfig;
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
    private final ObjectMapper om; // 주입받는 전역 ObjectMapper
    private final PdfProperties pdfProps;

    @Value("${app.analysis.session-ttl-minutes:30}")
    private long ttlMinutes;

    /** 세션별 상태(유형별 횟수만 누적) */
    static class SessionState {
        Long ownerUserId;
        Long analId;                                   // 이 세션이 연결된 분석번호
        final EnumMap<FieldName, Integer> counts = new EnumMap<>(FieldName.class);
        volatile long received = 0;
        volatile Long total = null;
        volatile Instant startedAt = null;
        volatile Instant endedAt   = null;
        volatile long lastUpdated  = System.currentTimeMillis();
    }

    /** sid -> 세션 상태 */
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();
    
    /** analId ↔ sid 매핑 (세션 생성 시 기록) */
    private final Map<Long, String> analIdToSid = new ConcurrentHashMap<>();

    /* =====================================================================
       시작 API (컨트롤러와 호환을 위해 2가지 시그니처 제공)
       ===================================================================== */

    /** 컨트롤러에서 files를 받지만, 여기서는 생성만 수행(파일은 선택적/미사용) */
    public String beginSession(Long userId, List<MultipartFile> files) {
        // user 검증
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));

        // TB_ANALYSIS 생성(시작 시각 now)
        Analysis a = new Analysis();
        a.setUser(u);
        a.setCreatedAt(Timestamp.from(Instant.now()));
        // finishedAt/analResult/analRate/reportUrl 등은 콜백에서 세팅
        a = analysisRepository.save(a);

        // 세션 생성
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

    /** 이미 만들어둔 analId가 있으면 그것과 세션을 연결할 때 사용 (호환용) */
    public String beginSession(Long userId, Long analId) {
        Long resolvedAnalId = analId;

        if (resolvedAnalId == null) {
            // 없으면 새로 생성
            User u = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
            Analysis a = new Analysis();
            a.setUser(u);
            a.setCreatedAt(Timestamp.from(Instant.now()));
            a = analysisRepository.save(a);
            resolvedAnalId = a.getAnalIdx();
        } else {
            // 있어도 존재 검증(없으면 예외)
            if (!analysisRepository.existsById(resolvedAnalId)) {
                throw new IllegalArgumentException("analysis not found: " + resolvedAnalId);
            }
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

    /* =====================================================================
       세션/집계/상태
       ===================================================================== */

    public Long getAnalIdForSid(String sid) {
        SessionState st = sessions.get(sid);
        return (st != null) ? st.analId : null;
    }

    /** sid → userId (없으면 null) */
    public Long getUserIdForSid(String sid) {
        SessionState st = sessions.get(sid);
        return (st != null) ? st.ownerUserId : null;
    }
    
    /** analId → sid 조회 (없으면 null) */
    public String getSidForAnalId(Long analId) {
        return analIdToSid.get(analId);
    }

    /** 모델 콜백에서 “유형만” 누적 */
    public long appendTypes(String sid, List<FieldName> types, Long total) {
        if (sid == null || sid.isBlank()) throw new IllegalArgumentException("sid 누락");
        SessionState st = sessions.computeIfAbsent(sid, k -> new SessionState());
        if (types != null) {
            for (FieldName t : types) {
                if (t == null) continue;
                st.counts.merge(t, 1, Integer::sum);
                st.received++; // 입력 아이템 수(옵션)
            }
        }
        if (total != null) st.total = total;
        st.lastUpdated = System.currentTimeMillis();
        return st.counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** 세션 종료: endedAt 저장 (DB 반영은 콜백 컨트롤러가 수행) */
    public void markEnded(String sid) {
        SessionState st = sessions.get(sid);
        if (st != null) {
            st.endedAt = Instant.now();
            st.lastUpdated = System.currentTimeMillis();
        }
    }

    /** 현재 상태 조회 */
    public AnalysisStatusDto getStatus(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return new AnalysisStatusDto(0, 0, null);
        long processed = st.counts.values().stream().mapToInt(Integer::intValue).sum();
        return new AnalysisStatusDto(st.received, processed, st.total);
    }

    /** 유형별 횟수 맵 반환(불변 사본) */
    public Map<FieldName, Integer> getTypeCounts(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return Map.of();
        EnumMap<FieldName, Integer> copy = new EnumMap<>(FieldName.class);
        copy.putAll(st.counts);
        return Collections.unmodifiableMap(copy);
    }

    /** 등급 산정: 횟수만으로 계산(튜닝 포인트) */
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

    /** 세션 TTL 청소 */
    @Scheduled(fixedDelay = 60_000)
    void cleanup() {
        long now = System.currentTimeMillis();
        long ttlMs = Math.max(1, ttlMinutes) * 60_000L;
        sessions.entrySet().removeIf(e -> now - e.getValue().lastUpdated > ttlMs);
    }

    /** sid로 username 복원 (없으면 null) */
    public String getUsernameForSid(String sid) {
        Long userId = getUserIdForSid(sid);
        if (userId == null) return null;
        return userRepository.findMemberUsernameByUserId(userId).orElse(null);
    }

    /* =====================================================================
       ▼▼▼ 콜백 저장: 컨트롤러에서 호출하는 메서드 (신규) ▼▼▼
       ===================================================================== */

    /**
     * 모델 콜백 결과를 받아서 유형 집계 → 등급 산정 → Analysis 갱신.
     * @param analId  분석 ID
     * @param results JSON 배열을 파싱한 결과(List<Map>) - 컨트롤러에서 그대로 전달
     */
    // 기존 saveFromCallback를 누적 저장으로 변경
    public void saveFromCallback(Long analId, List<Map<String, Object>> results) {
        if (analId == null) throw new IllegalArgumentException("analId is null");
        if (results == null || results.isEmpty()) return;

        // 1) 이번 콜백에서 추가되는 카운트 계산
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

        // 2) 기존 저장값 읽어와 누적 merge
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("analysis not found: " + analId));

        Map<FieldName, Integer> oldCounts = parseCountsFromAnalResult(a.getAnalResult());
        long oldReceived = parseReceivedFromAnalResult(a.getAnalResult());

        EnumMap<FieldName, Integer> merged = new EnumMap<>(FieldName.class);
        // 기존 + 추가
        for (FieldName f : FieldName.values()) {
            int base = oldCounts.getOrDefault(f, 0);
            int inc  = addCounts.getOrDefault(f, 0);
            if (base + inc > 0) merged.put(f, base + inc);
        }
        long newReceived = Math.max(0, oldReceived) + Math.max(0, addReceived);

        // 3) 등급 재산정 후 저장
        AnalRate rate = gradeByCounts(merged);
        try {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("counts", toStringKeyMap(merged)); // {"VIOLENCE":n, ...}
            summary.put("received", newReceived);

            a.setAnalRate(rate);
            a.setAnalResult(om.writeValueAsString(summary));
            // a.setFinishedAt(...); // 필요시
            analysisRepository.save(a);
        } catch (Exception e) {
            throw new RuntimeException("failed to persist merged analysis result", e);
        }
    }
    
    
    /** analId 기반으로 n8n 전송 + DB 누적 보정까지 수행 */
 // AnalysisResultService.java (핵심 부분만)
    public boolean finalizeByAnalId(Long analId) {
        Analysis a = analysisRepository.findById(analId)
            .orElseThrow(() -> new IllegalArgumentException("analysis not found: " + analId));

        Map<FieldName,Integer> counts = parseCountsFromAnalResult(a.getAnalResult());
        AnalRate rate = gradeByCounts(counts);

        Long userId = (a.getUser()!=null) ? a.getUser().getUserIdx() : null;
        String username = (userId!=null)
                ? userRepository.findMemberUsernameByUserId(userId).orElse("")
                : "";

        Instant started = a.getCreatedAt()!=null ? a.getCreatedAt().toInstant() : null;
        Instant ended   = a.getFinishedAt()!=null ? a.getFinishedAt().toInstant(): null;

        String callbackUrl = buildCallbackUrl(analId); // 설정 기반 (없어도 됨)

        return pdfWebhookClient.dispatchCountsWithAnalId(
            analId, username, null, counts, rate, started, ended, callbackUrl
        );
    }

    private String buildCallbackUrl(Long analId) {
        if (analId == null) return null;
        String tpl = pdfProps.getCallbackUrlTemplate();
        if (tpl != null && !tpl.isBlank()) return tpl.replace("{analId}", String.valueOf(analId));
        String base = pdfProps.getPublicBaseUrl();
        if (base != null && !base.isBlank()) return base.replaceAll("/+$","") + "/api/analysis/" + analId + "/pdf-callback";
        return null; // 콜백 없이도 동작
    }

    
    /**
     * 세션을 끝내면서 웹훅 전송 + DB에도 누적 반영(세션 카운트 → merge 저장)
     */
    public boolean finalizeNow(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return false;

        
        Map<FieldName,Integer> counts = getTypeCounts(sid);
        AnalRate rate = gradeByCounts(counts);
        String username = Optional.ofNullable(getUsernameForSid(sid)).orElse("");

        boolean ok = pdfWebhookClient.dispatchCountsWithAnalId(
                st.analId, username, null, sid, counts, rate, st.startedAt, st.endedAt
        );

        // DB 누적 반영…
        if (st.analId != null) persistMergeToAnalysis(st.analId, counts, st.received);
        return ok;
    }

    // ====== 내부 유틸 ======

    /** 기존 analResult에서 counts/received 파싱 */
    private Map<FieldName, Integer> parseCountsFromAnalResult(String json) {
        try {
            if (json == null || json.isBlank()) return Map.of();
            var root = om.readTree(json);
            var node = root.hasNonNull("results") ? root.get("results") : root;

            // counts/typeCounts 우선
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

            // 배열이면 스캔하여 집계(레거시 호환)
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

    /** Map<FieldName,Integer> → Map<String,Integer> (JSON 저장용) */
    private static Map<String, Integer> toStringKeyMap(Map<FieldName, Integer> m) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (var e : m.entrySet()) out.put(e.getKey().name(), e.getValue());
        return out;
    }

    /** DB에 누적 merge (세션/콜백 공용) */
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

    // extractTypes / toFieldName 등 기존 유틸은 그대로 사용

    /* ==============================
       파싱/매핑 유틸
       ============================== */

    /** 단일 결과 아이템에서 FieldName 리스트 추출 */
    @SuppressWarnings("unchecked")
    private static List<FieldName> extractTypes(Map<String, Object> item) {
        List<FieldName> out = new ArrayList<>();

        // 1) labels: ["VIOLENCE","DEFAMATION",...]
        Object labels = item.get("labels");
        if (labels instanceof List<?>) {
            addFromList(out, (List<?>) labels);
        }

        // 2) classification: ["VIOLENCE",...] 또는 [{label:"VIOLENCE"}, {name:"..."}]
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

        // 3) 단일 키: label/type/category
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

    /** 문자열 → FieldName 매핑 (대소문자 무시 + 일부 별칭 보정) */
    private static FieldName toFieldName(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase(Locale.ROOT);

        // 별칭/동의어 보정
        switch (s) {
            case "HARASSMENT" -> s = "BULLYING";
            case "BLACKMAIL"  -> s = "CHANTAGE";
            case "VIOLENT"    -> s = "VIOLENCE";
        }

        try {
            return FieldName.valueOf(s);
        } catch (Exception ignore) {
            return null;
        }
    }
}
