package com.smhrd.dtect.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
        volatile long received = 0;   // 받은 프레임 수
        volatile long processed = 0;  // 모델 결과 반영된 항목 수 (여기선 items.size()와 같게 유지)
        volatile long lastUpdated = System.currentTimeMillis();
    }

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    private SessionState getOrCreate(String sid) {
        return sessions.computeIfAbsent(sid, k -> new SessionState());
    }

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
            st.items.addAll(results);
        }
        st.processed = st.items.size();
        st.lastUpdated = System.currentTimeMillis();
        return st.received;
    }

    public AnalysisStatusDto getStatus(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return new AnalysisStatusDto(0, 0, null);
        return new AnalysisStatusDto(st.received, st.processed, null); // total 미정
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