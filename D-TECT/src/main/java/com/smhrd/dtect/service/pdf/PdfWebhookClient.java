package com.smhrd.dtect.service.pdf;

import com.smhrd.dtect.config.PdfProperties;
import com.smhrd.dtect.dto.LabelCount;
import com.smhrd.dtect.dto.ModelMessage;
import com.smhrd.dtect.entity.AnalRate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfWebhookClient {

    private final PdfProperties pdfProps;

    // 외부 빈 없이 자체 생성해도 됩니다(빈 주입 원하면 교체 가능)
    private final WebClient webClient = WebClient.builder().build();

    /**
     * n8n(Webhook)으로 {"results": {...}} 형태의 JSON을 전송
     */
    public boolean dispatchJson(Long userId,
                                String sid,
                                List<ModelMessage> items,
                                AnalRate rate,
                                Long analIdOrNull,
                                String reportPathOrNull) {

        final String url = pdfProps.getWebhookUrl();
        if (url == null || url.isBlank()) {
            log.warn("[PdfWebhook] webhookUrl not configured; skip dispatch (stub). sid={}", sid);
            return true;
        }

        boolean empty = (items == null || items.isEmpty());

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("anal_idx", analIdOrNull != null ? String.valueOf(analIdOrNull) : sid);
        results.put("sid", sid);
        results.put("userId", userId); // 세션에서 복원된 Long userId가 들어옵니다
        results.put("createdAt", java.time.Instant.now().toString());
        results.put("reportPath", reportPathOrNull != null ? reportPathOrNull : "");
        results.put("items", empty ? List.of() : items);
        results.put("isEmpty", empty);

        if (empty) {
            results.put("anal_rate", AnalRate.NORMAL.name());
            results.put("anal_result", "수신된 모델 데이터가 없어 요약할 항목이 없습니다.");
        } else {
            results.put("anal_rate", (rate != null ? rate.name() : AnalRate.NORMAL.name()));
            results.put("anal_result", buildTextSummary(items));
        }

        Map<String, Object> body = Map.of("results", results);

        Boolean ok = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(resp -> resp.bodyToMono(String.class).defaultIfEmpty("")
                        .map(b -> {
                            if (resp.statusCode().is2xxSuccessful()) {
                                log.info("[PdfWebhook] OK {} sid={} count={} body={}",
                                        resp.statusCode(), sid,
                                        (items != null ? items.size() : 0),
                                        b.length() > 500 ? b.substring(0, 500) + "...(truncated)" : b);
                                return true;
                            } else {
                                log.error("[PdfWebhook] FAIL {} sid={} body={}",
                                        resp.statusCode(), sid, b);
                                return false;
                            }
                        }))
                .onErrorResume(e -> {
                    log.error("[PdfWebhook] dispatch exception sid={} err={}", sid, e.toString());
                    return reactor.core.publisher.Mono.just(false);
                })
                .block();

        return Boolean.TRUE.equals(ok);
    }

    // ===== 내부 유틸 =====

    private static String buildTextSummary(List<ModelMessage> items) {
        if (items == null || items.isEmpty()) return "감지된 유의미한 항목이 없습니다.";
        final int TOP = 20;
        StringBuilder sb = new StringBuilder(8_000);

        items.stream()
                .sorted(Comparator.comparingDouble(m -> parseScore(((ModelMessage) m).getScore())).reversed())
                .limit(TOP)
                .forEach(m -> {
                    LabelCount top = topLabel(m.getClassification());
                    String label = (top != null && top.getLabel() != null) ? top.getLabel() : "UNKNOWN";
                    int count    = (top != null) ? top.getCount() : 0;
                    String user  = nvl(m.getUser(), "—");
                    String text  = nvl(m.getText(), "—"); // 원문 그대로(줄바꿈/escape는 n8n에서)
                    String score = fmt(m.getScore());

                    sb.append("• [").append(label).append("] score=").append(score)
                      .append(", count=").append(count)
                      .append(", user=").append(user)
                      .append(" — ").append(text)
                      .append("\n");
                });

        return sb.toString();
    }

    private static LabelCount topLabel(List<LabelCount> list) {
        if (list == null || list.isEmpty()) return null;
        return list.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(LabelCount::getCount))
                .orElse(null);
    }

    private static String nvl(String s, String d){ return (s == null || s.isBlank()) ? d : s; }

    private static double parseScore(String s) {
        try { return (s != null) ? Double.parseDouble(s) : 0d; }
        catch (Exception e) { return 0d; }
    }

    private static String fmt(String s){
        try { return String.format(java.util.Locale.US, "%.2f", Double.parseDouble(s)); }
        catch(Exception e){ return "0.00"; }
    }
}
