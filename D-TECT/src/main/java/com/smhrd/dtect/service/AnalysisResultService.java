package com.smhrd.dtect.service;

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

        return sid;
    }
    
    /** 이미 만들어둔 analId가 있으면 그것과 세션을 연결할 때 사용 (호환용) */
    public String beginSession(Long userId, Long analId) {
        // 1) 분석 ID를 해석: 있으면 검증, 없으면 새로 생성
    	Long resolvedAnalId = analId;
        
        if (resolvedAnalId == null) {
            var user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));

            var a = new Analysis();
            a.setUser(user);
            a.setCreatedAt(new Timestamp(Instant.now().toEpochMilli()));
            resolvedAnalId = analysisRepository.save(a).getAnalIdx();
        } else {
        	if (!analysisRepository.existsById(resolvedAnalId)) {
                throw new IllegalArgumentException("analysis not found: " + resolvedAnalId);
            }
        }

        // 2) 세션 구성
        String sid = UUID.randomUUID().toString().replace("-", "");
        SessionState st = new SessionState();
        st.ownerUserId = userId;
        st.analId      = resolvedAnalId;
        st.startedAt   = Instant.now();
        st.lastUpdated = System.currentTimeMillis();
        sessions.put(sid, st);

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

    /** n8n으로 “횟수만” 전송 (즉시 전송 유틸) */
    public boolean finalizeNow(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return false;

        Map<FieldName, Integer> counts = getTypeCounts(sid);
        AnalRate rate = gradeByCounts(counts);

        String username = getUsernameForSid(sid);
        if (username == null) username = "";

        return pdfWebhookClient.dispatchCounts(
                username,  // userId 대신 username
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
