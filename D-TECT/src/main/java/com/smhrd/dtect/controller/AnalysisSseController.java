package com.smhrd.dtect.controller;

import com.smhrd.dtect.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisSseController {

    private final AnalysisRepository analysisRepository;
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping("/{analId}/events")
    public SseEmitter subscribe(@PathVariable Long analId) {
        SseEmitter emitter = new SseEmitter(60_000L); // 60초 타임아웃
        emitters.put(analId, emitter);

        emitter.onCompletion(() -> emitters.remove(analId));
        emitter.onTimeout(() -> emitters.remove(analId));

        try {
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data(Map.of("state", "waiting")));
        } catch (IOException ignored) {}

        return emitter;
    }

    /** PDF 업로드 완료 → 클라이언트에 즉시 알림 */
    public void notifyReady(Long analId, String reportUrl) {
        SseEmitter emitter = emitters.get(analId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("status")
                        .data(Map.of("state", "ready", "reportUrl", reportUrl)));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }
    }
}
