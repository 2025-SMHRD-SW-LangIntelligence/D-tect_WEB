package com.smhrd.dtect.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smhrd.dtect.dto.AnalysisStatusDto;
import com.smhrd.dtect.dto.ModelResultDto;
import com.smhrd.dtect.service.model.ModelGateway;

@Service
public class AnalysisResultService {

    private final ModelGateway modelGateway;

    public AnalysisResultService(ModelGateway modelGateway) {
        this.modelGateway = modelGateway; // ✅ final 필드 초기화
    }

    @Value("${app.analysis.session-ttl-minutes:30}")
    private long ttlMinutes;

    private static class SessionState {
        final List<ModelResultDto> items = Collections.synchronizedList(new ArrayList<>());
        final Set<String> fingerprints = ConcurrentHashMap.newKeySet(); // ✅ 중복 방지
        volatile long received = 0;      // 받은 프레임 수 (pull 모드)
        volatile long processed = 0;     // 누적 결과 수 (items.size)
        volatile Long total = null;      // (선택) 전체 예상 개수 (push 모드에서 활용)
        volatile long lastUpdated = System.currentTimeMillis();
    }

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    private SessionState getOrCreate(String sid) {
        return sessions.computeIfAbsent(sid, k -> new SessionState());
    }

    // ===== Pull 모드: 우리가 프레임을 업로드하고 모델을 동기로 호출 =====
    public long appendFrame(String sid, MultipartFile file) throws Exception {
        if (sid == null || sid.isBlank()) throw new IllegalArgumentException("sid 누락");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("이미지 누락");

        SessionState st = getOrCreate(sid);
        st.received++;

        byte[] bytes = file.getBytes(); // JPEG 권장
        String name = file.getOriginalFilename();

        List<ModelResultDto> results;
        try {
            results = modelGateway.predict(bytes, name);
        } catch (Exception e) {
            // 모델 서버 오류 시에도 세션은 유지. 로깅 후 빈 결과 반영.
            results = List.of();
        }

        if (results != null && !results.isEmpty()) {
            for (ModelResultDto r : results) {
                if (r == null) continue;
                String fp = fingerprint(r);
                if (st.fingerprints.add(fp)) {
                    st.items.add(r);
                }
            }
        }
        st.processed = st.items.size();
        st.lastUpdated = System.currentTimeMillis();
        return st.received;
    }

    // ===== Push 모드: 모델이 콜백으로 결과를 보내줄 때 사용 =====
    public long appendResults(String sid, List<ModelResultDto> results) {
        return appendResults(sid, results, null);
    }

    public long appendResults(String sid, List<ModelResultDto> results, Long total) {
        if (sid == null || sid.isBlank()) throw new IllegalArgumentException("sid 누락");
        SessionState st = getOrCreate(sid);

        if (results != null && !results.isEmpty()) {
            int added = 0;
            for (ModelResultDto r : results) {
                if (r == null) continue;
                String fp = fingerprint(r);
                if (st.fingerprints.add(fp)) {
                    st.items.add(r);
                    added++;
                }
            }
        }

        if (total != null) st.total = total;

        st.processed = st.items.size();
        st.lastUpdated = System.currentTimeMillis();
        return st.processed;
    }

    private static String fingerprint(ModelResultDto r) {
        // 텍스트/라벨/카운트/스코어/유저로 간단 키 생성 (필요시 SHA-256로 강화 가능)
        String label = (r.classification() != null) ? r.classification().label() : "";
        int count    = (r.classification() != null) ? r.classification().count() : 0;
        String score = (r.score() != null) ? r.score() : "";
        String text  = (r.text() != null) ? r.text() : "";
        String user  = (r.user() != null) ? r.user() : "";
        return user + "\u0001" + text + "\u0001" + label + "\u0001" + count + "\u0001" + score;
    }

    // ===== 상태/결과 조회 =====
    public AnalysisStatusDto getStatus(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return new AnalysisStatusDto(0, 0, null);
        return new AnalysisStatusDto(st.received, st.processed, st.total); // total 반영
    }

    public List<ModelResultDto> getResult(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return List.of();
        synchronized (st.items) {
            return List.copyOf(st.items);
        }
    }

    public void clear(String sid) {
        sessions.remove(sid);
    }

    // TTL 청소
    @Scheduled(fixedDelay = 60_000)
    void cleanup() {
        long now = System.currentTimeMillis();
        long ttlMs = Math.max(1, ttlMinutes) * 60_000L;
        sessions.entrySet().removeIf(e -> now - e.getValue().lastUpdated > ttlMs);
    }
}
