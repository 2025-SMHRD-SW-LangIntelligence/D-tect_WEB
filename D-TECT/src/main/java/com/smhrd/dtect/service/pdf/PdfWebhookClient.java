package com.smhrd.dtect.service.pdf;

import com.smhrd.dtect.config.PdfProperties;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.FieldName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfWebhookClient {

    private final PdfProperties pdfProps;
    private final WebClient webClient = WebClient.builder().build();

    /**
     * n8n(Webhook)으로 "유형별 횟수"만 전송
     * body 예시:
     * {
     *   "userId": 123,
     *   "sid": "abc123",
     *   "period": { "startedAt": "2025-08-29T09:00:00Z", "endedAt": "2025-08-29T10:00:00Z" },
     *   "typeCounts": {"VIOLENCE":30,"SEXUAL":10,"THREAT":2,"STALKING":3},
     *   "analRate": "WARNING"
     * }
     */
    /** username 기반 페이로드 전송 */
    public boolean dispatchCounts(
            String username,
            String sid,
            Map<FieldName, Integer> typeCounts,
            AnalRate analRate,
            Instant startedAt,
            Instant endedAt
    ) {
        final String url = pdfProps.getWebhookUrl();
        if (url == null || url.isBlank()) {
            log.warn("[PdfWebhook] webhookUrl not configured; skip dispatch (stub). sid={}", sid);
            return true;
        }

        var body = Map.of(
            "username", username,
            "sid", sid,
            "period", Map.of(
                "startedAt", startedAt != null ? startedAt.toString() : null,
                "endedAt",   endedAt   != null ? endedAt.toString()   : null
            ),
            "typeCounts", typeCounts != null ? typeCounts : Map.of(),
            "analRate",   (analRate != null ? analRate : AnalRate.NORMAL).name()
        );

        Boolean ok = webClient.post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchangeToMono(resp -> resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(b -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        log.info("[PdfWebhook] OK {} sid={} username={} sentCounts={}",
                                resp.statusCode(), sid, username,
                                (typeCounts != null ? typeCounts.values().stream().mapToInt(Integer::intValue).sum() : 0));
                        return true;
                    } else {
                        log.error("[PdfWebhook] FAIL {} sid={} body={}", resp.statusCode(), sid, b);
                        return false;
                    }
                })
            )
            .onErrorResume(e -> {
                log.error("[PdfWebhook] dispatch exception sid={} err={}", sid, e.toString());
                return reactor.core.publisher.Mono.just(false);
            })
            .block();

        return Boolean.TRUE.equals(ok);
    }
}