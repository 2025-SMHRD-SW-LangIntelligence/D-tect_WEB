package com.smhrd.dtect.service;

import com.smhrd.dtect.dto.AnalysisStatusDto;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.FieldName;
import com.smhrd.dtect.repository.UserRepository;
import com.smhrd.dtect.service.pdf.PdfWebhookClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AnalysisResultService {

    private final PdfWebhookClient pdfWebhookClient;
    private final UserRepository userRepository;

    @Value("${app.analysis.session-ttl-minutes:30}")
    private long ttlMinutes;

    /** 세션별 상태(유형별 횟수만 누적) */
    static class SessionState {
        Long ownerUserId;
        Long analId;                 // ✅ 추가: 이 세션이 연결된 분석번호
        final EnumMap<FieldName,Integer> counts = new EnumMap<>(FieldName.class);
        volatile long received = 0;
        volatile Long total = null;
        volatile Instant startedAt = null;
        volatile Instant endedAt   = null;
        volatile long lastUpdated = System.currentTimeMillis();
    }

    // ✅ analId를 함께 저장하는 시작 API
    public String beginSession(Long userId, Long analId) {
        String sid = UUID.randomUUID().toString().replace("-", "");
        SessionState st = new SessionState();
        st.ownerUserId = userId;
        st.analId = analId;
        st.startedAt = Instant.now();
        sessions.put(sid, st);
        return sid;
    }

    public Long getAnalIdForSid(String sid) {
        SessionState st = sessions.get(sid);
        return (st != null) ? st.analId : null;
    }

    /** 모델 콜백에서 “유형만” 누적 */
    public long appendTypes(String sid, List<FieldName> types, Long total) {
        if (sid == null || sid.isBlank()) throw new IllegalArgumentException("sid 누락");
        SessionState st = sessions.computeIfAbsent(sid, k -> new SessionState());
        if (types != null) {
            for (FieldName t : types) {
                if (t == null) continue;
                st.counts.merge(t, 1, Integer::sum);
                st.received++; // 필요시 의미: 들어온 아이템 수(선택)
            }
        }
        if (total != null) st.total = total;
        st.lastUpdated = System.currentTimeMillis();
        return st.counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** 세션 종료: endedAt 저장 */
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
    public Map<FieldName,Integer> getTypeCounts(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return Map.of();
        EnumMap<FieldName,Integer> copy = new EnumMap<>(FieldName.class);
        copy.putAll(st.counts);
        return Collections.unmodifiableMap(copy);
    }

    /** 등급 산정: 횟수만으로 계산 */
    public AnalRate gradeByCounts(Map<FieldName,Integer> counts) {
        if (counts == null || counts.isEmpty()) return AnalRate.NORMAL;

        // 유형 가중치(예시): 운영 데이터로 튜닝하세요
        Map<FieldName,Integer> W = Map.of(
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

        // 임계값(예시)
        if (weighted >= 30) return AnalRate.DANGER;
        if (weighted >= 15) return AnalRate.WARNING;
        return AnalRate.NORMAL;
    }

    /** n8n으로 “횟수만” 전송 (즉시 전송 유틸) */
    public boolean finalizeNow(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return false;

        Map<FieldName, Integer> counts = getTypeCounts(sid);
        AnalRate rate = gradeByCounts(counts);

        // ✅ username 조회 (null/blank면 빈 문자열로 방어)
        String username = getUsernameForSid(sid);
        if (username == null) username = "";

        return pdfWebhookClient.dispatchCounts(
            username,  // ✅ userId 대신 username
            sid,
            counts,
            rate,
            st.startedAt,
            st.endedAt
        );
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
    
    
}
