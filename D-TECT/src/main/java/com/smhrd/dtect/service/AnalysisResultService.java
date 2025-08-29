package com.smhrd.dtect.service;

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


    // 모델링!!
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
                    FieldName fn = FieldName.valueOf(lc.getLabel().toUpperCase());
                    Case c = new Case();
                    c.setAnalysis(analysis);
                    c.setCaseType(fn);           // 중복 허용: 요구사항 그대로 저장
                    toSave.add(c);
                } catch (IllegalArgumentException ignore) {
                    // FieldName에 없는 라벨은 무시
                }
            }
        }
        if (!toSave.isEmpty()) {
            caseRepository.saveAll(toSave);
            log.info("Saved {} CASE(s) for analId={}", toSave.size(), analId);
        }
    }

    // 모델링!!!

    @Transactional
    public void saveFromCallback(Long analId, List<ModelMessage> results) {
        Analysis analysis = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis not found: " + analId));

        List<Case> toSave = new ArrayList<>();
        for (ModelMessage m : results) {
            if (m.getClassification() == null) continue;
            for (LabelCount lc : m.getClassification()) {
                if (lc.getCount() <= 0) continue;
                try {
                    FieldName fn = FieldName.valueOf(lc.getLabel().toUpperCase());
                    Case c = new Case();
                    c.setAnalysis(analysis);
                    c.setCaseType(fn);
                    toSave.add(c);
                } catch (IllegalArgumentException e) {
                }
            }
        }
        if (!toSave.isEmpty()) {
            caseRepository.saveAll(toSave);
        }
    }

    private FieldName pickTopLabel(ModelMessage m) {
        if (m.getClassification() == null || m.getClassification().isEmpty()) {
            return null;
        }
        LabelCount best = m.getClassification().stream()
                .max(Comparator.comparingInt(lc -> lc.getCount()))
                .orElse(null);
        if (best == null) return null;

        String raw = (best.getLabel() == null) ? "" : best.getLabel().trim().toUpperCase();

        try {
            return FieldName.valueOf(raw);
        } catch (IllegalArgumentException iae) {
            // 라벨이 enum에 없으면 스킵 (FieldName에 UNKNOWN이 있다면 그걸 쓰는 방식도 가능)
            log.warn("Unknown label from model: '{}'", raw);
            return null;
        }
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
