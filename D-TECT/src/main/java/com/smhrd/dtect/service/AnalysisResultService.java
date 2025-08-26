package com.smhrd.dtect.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
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
import com.smhrd.dtect.dto.ModelMessage;
import com.smhrd.dtect.dto.ModelResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 분석 세션 상태 및 결과 저장소.
 * - push 콜백/중간 처리 결과를 세션별로 누적
 * - 내부 표준 타입은 ModelMessage 로 통일
 */
@Service
@Slf4j
public class AnalysisResultService {

    @Value("${app.analysis.session-ttl-minutes:30}")
    private long ttlMinutes;

    private static class SessionState {
        final List<ModelMessage> items = Collections.synchronizedList(new ArrayList<>());
        final Set<String> fingerprints = ConcurrentHashMap.newKeySet();
        volatile long received = 0;     // 입력 프레임 수 (pull 모드 추정)
        volatile long processed = 0;    // items.size
        volatile Long total = null;     // 총 예상 개수(있을 때만 사용)
        volatile long lastUpdated = System.currentTimeMillis();
        volatile Long userId = null;
    }

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    /** 새 세션 시작 */
    public String beginSession(Long userId, List<MultipartFile> files) {
        String sid = "S" + Long.toHexString(System.currentTimeMillis());
        SessionState st = new SessionState();
        st.userId = userId;
        sessions.put(sid, st);
        log.info("[Analysis] begin sid={}, files={}", sid, files != null ? files.size() : 0);

        // TODO: 파일 저장/큐잉, OCR 파이프라인 킥오프 등
        return sid;
    }

    /** 진행상태 조회 */
    public AnalysisStatusDto getStatus(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) {
            // not found: done = true 로 표시
            return new AnalysisStatusDto(sid, 0, 0L, 0L, 0, 0, true);
        }
        int size = st.items.size();
        long now = System.currentTimeMillis();
        long ageSec = (now - st.lastUpdated) / 1000L;
        long total = st.total != null ? st.total : size;
        int progress = (total > 0) ? (int) Math.min(100, (size * 100 / total)) : 0;

        return new AnalysisStatusDto(
                sid,
                progress,
                st.received,
                (long) size,
                (int) total,
                (int) ageSec,
                false
        );
    }

    /** 최종 결과 목록 반환 */
    public List<ModelMessage> getResult(String sid) {
        SessionState st = sessions.get(sid);
        return st != null ? new ArrayList<>(st.items) : Collections.emptyList();
    }

    /** (핵심) 모델 콜백(push)에서 받은 ModelResponse 리스트를 내부 표준(ModelMessage)로 변환하여 누적 */
    public void appendResponses(String sid, List<ModelResponse> responses) {
        if (responses == null || responses.isEmpty()) return;
        SessionState st = sessions.computeIfAbsent(sid, k -> new SessionState());

        int appended = 0;
        for (ModelResponse r : responses) {
            if (r == null) continue;
            ModelMessage m = ModelMessage.builder()
                    .user(r.getUser())
                    .text(r.getText())
                    .score(r.getScore())
                    .classification(r.getClassification())
                    .build();

            String fp = fingerprint(m);
            if (st.fingerprints.add(fp)) {
                st.items.add(m);
                appended++;
            }
        }
        st.processed = st.items.size();
        st.lastUpdated = System.currentTimeMillis();
        log.info("[Analysis] sid={} appended={}, total={}", sid, appended, st.items.size());
    }

    /** pull 방식 등에서 단건 처리 기록(이미 표준형 ModelMessage가 들어오는 경우) */
    public void recordProcessed(String sid, ModelMessage message) {
        SessionState st = sessions.computeIfAbsent(sid, k -> new SessionState());
        st.received++;
        if (message != null) {
            String fp = fingerprint(message);
            if (st.fingerprints.add(fp)) {
                st.items.add(message);
            }
            st.processed = st.items.size();
        }
        st.lastUpdated = System.currentTimeMillis();
    }

    /** 세션 정리 */
    public void close(String sid) {
        sessions.remove(sid);
    }

    /** TTL 기반 GC */
    @Scheduled(fixedDelay = 300_000L, initialDelay = 300_000L)
    public void gc() {
        long now = System.currentTimeMillis();
        long ttlMs = ttlMinutes * 60_000L;
        int removed = 0;
        for (var it = sessions.entrySet().iterator(); it.hasNext();) {
            var e = it.next();
            SessionState st = e.getValue();
            if (now - st.lastUpdated > ttlMs) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.info("[Analysis] GC removed {} expired sessions", removed);
        }
    }

    /** 텍스트 + 라벨/카운트 조합 해시로 중복방지 */
    private static String fingerprint(ModelMessage m) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String raw = (m.getText() != null ? m.getText() : "") + "|" + String.valueOf(m.getClassification());
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return (m.getText() != null ? m.getText() : "") + "|" + String.valueOf(m.getClassification()).hashCode();
        }
    }
}
