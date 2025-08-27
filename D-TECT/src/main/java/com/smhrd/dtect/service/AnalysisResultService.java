package com.smhrd.dtect.service;

import com.smhrd.dtect.dto.*;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.service.pdf.PdfWebhookClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AnalysisResultService {

    private final PdfWebhookClient pdfWebhookClient;

    @Value("${app.analysis.session-ttl-minutes:30}")
    private long ttlMinutes;

    static class SessionState {
        Long ownerUserId; // Long user_idx
        final List<ModelMessage> items = Collections.synchronizedList(new ArrayList<>());
        final Set<String> fingerprints = ConcurrentHashMap.newKeySet();
        volatile long received = 0, processed = 0;
        volatile Long total = null;
        volatile long lastUpdated = System.currentTimeMillis();
    }

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    public String beginSession(Long userId, List<MultipartFile> files) {
        String sid = UUID.randomUUID().toString().replace("-", "");
        SessionState st = new SessionState();
        st.ownerUserId = userId;
        st.lastUpdated = System.currentTimeMillis();
        sessions.put(sid, st);
        return sid;
    }

    public Long getUserIdForSid(String sid) {
        SessionState st = sessions.get(sid);
        return (st != null) ? st.ownerUserId : null;
    }

    public long appendResults(String sid, List<ModelMessage> results, Long total) {
        if (sid == null || sid.isBlank()) throw new IllegalArgumentException("sid 누락");
        SessionState st = sessions.computeIfAbsent(sid, k -> new SessionState());
        if (results != null) {
            for (ModelMessage r : results) {
                String fp = fingerprint(r);
                if (st.fingerprints.add(fp)) st.items.add(r);
            }
        }
        if (total != null) st.total = total;
        st.processed = st.items.size();
        st.lastUpdated = System.currentTimeMillis();
        return st.processed;
    }

    public boolean finalizeNow(String sid) {
        SessionState st = sessions.get(sid);
        List<ModelMessage> items = (st != null) ? copyOf(st.items) : List.of();
        AnalRate rate = AnalysisGrader.grade(items);
        Long userId = (st != null) ? st.ownerUserId : null;
        return pdfWebhookClient.dispatchJson(userId, sid, items, rate, null, null);
    }

    public AnalysisStatusDto getStatus(String sid) {
        SessionState st = sessions.get(sid);
        if (st == null) return new AnalysisStatusDto(0, 0, null);
        return new AnalysisStatusDto(st.received, st.processed, st.total);
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

    // ===== util =====
    private static String fingerprint(ModelMessage r) {
        StringBuilder sb = new StringBuilder(256);
        sb.append(nvl(r.getUser())).append('\u0001')
          .append(nvl(r.getText())).append('\u0001')
          .append(nvl(r.getScore())).append('\u0001');
        var cls = r.getClassification();
        if (cls != null) {
            for (var lc : cls) {
                if (lc == null) continue;
                sb.append(nvl(lc.getLabel())).append(':').append(lc.getCount()).append('|');
            }
        }
        return sb.toString();
    }
    private static String nvl(String s){ return s == null ? "" : s; }
    private static List<ModelMessage> copyOf(List<ModelMessage> list){
        synchronized (list) { return List.copyOf(list); }
    }
}
