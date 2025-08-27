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
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfWebhookClient {

    // ✅ 외부 Config에서 주입받기 (PdfClientConfig 등에서 WebClient 빈 제공)
    private final WebClient pdfWebClient;
    private final PdfProperties pdfProps;

    /**
     * n8n Webhook이 const data = $input.first().json.results 로 읽을 수 있게
     * 최상위 바디에 {"results": {...}} 형태로 전송합니다.
     */
    public boolean dispatchJson(Long userId,
                                String sid,
                                List<ModelMessage> items,
                                AnalRate rate,
                                Long analIdOrNull,
                                String reportPathOrNull) {
        final String url = pdfProps.getWebhookUrl();

        // URL 미설정이면 스텁 모드: 파이프라인을 막지 않도록 true 반환
        if (url == null || url.isBlank()) {
            log.warn("[PdfWebhook] webhookUrl not configured; skip dispatch (stub mode). sid={}", sid);
            return true;
        }

        // n8n 템플릿이 기대하는 필드들 구성
        Map<String, Object> results = new LinkedHashMap<>();
        results.put("anal_idx", analIdOrNull != null ? String.valueOf(analIdOrNull) : sid);   // 문자열로
        results.put("anal_rate", rate != null ? rate.name() : "NORMAL");
        results.put("anal_result", buildTextSummary(items));                                   // 인쇄 친화 요약문
        results.put("reportPath", reportPathOrNull != null ? reportPathOrNull : "");
        results.put("sid", sid);
        results.put("userId", userId);
        results.put("createdAt", Instant.now().toString());
        results.put("items", items != null ? items : List.of());

        Map<String, Object> body = Map.of("results", results);

        // 응답 본문까지 로깅해서 n8n 내부 오류를 바로 파악
        Boolean ok = pdfWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(b -> {
                                    if (resp.statusCode().is2xxSuccessful()) {
                                        log.info("[PdfWebhook] OK {} sid={} count={} body={}",
                                                resp.statusCode(),
                                                sid,
                                                items != null ? items.size() : 0,
                                                trimBody(b));
                                        return true;
                                    } else {
                                        log.error("[PdfWebhook] FAIL {} url={} sid={} body={}",
                                                resp.statusCode(), url, sid, trimBody(b));
                                        return false;
                                    }
                                })
                )
                .onErrorResume(e -> {
                    log.error("[PdfWebhook] dispatch exception sid={} err={}", sid, e.toString());
                    return Mono.just(false);
                })
                .block();

        return Boolean.TRUE.equals(ok);
    }

    // ===== 내부 유틸 =====

    // 상위 20개만 간결한 텍스트 요약으로 생성
    private static String buildTextSummary(List<ModelMessage> items) {
        if (items == null || items.isEmpty()) return "감지된 유의미한 항목이 없습니다.";

        final int TOP = 20;
        StringBuilder sb = new StringBuilder(8_000);

        items.stream()
                .sorted(Comparator.comparingDouble(m -> parseScore(((ModelMessage) m).getScore())).reversed())
                .limit(TOP)
                .forEach(m -> {
                    LabelCount top = topLabel(m.getClassification()); // 다중 라벨 중 count 최댓값 1개 선택
                    String label = (top != null && top.getLabel() != null) ? top.getLabel() : "UNKNOWN";
                    int count    = (top != null) ? top.getCount() : 0;
                    String user  = nvl(m.getUser(), "—");
                    String text  = nvl(m.getText(), "—").replaceAll("\\s+", " ").trim();
                    String score = fmt(m.getScore());
                    sb.append("• [").append(label).append("] score=").append(score)
                      .append(", count=").append(count)
                      .append(", user=").append(user)
                      .append(" — ").append(text)
                      .append("\n");
                });

        return sb.toString();
    }

    // classification 배열에서 count 최댓값 1개
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
        try { return String.format(Locale.US, "%.2f", Double.parseDouble(s)); }
        catch(Exception e){ return "0.00"; }
    }

    private static String trimBody(String b) {
        if (b == null) return "";
        return b.length() > 1000 ? b.substring(0, 1000) + "...(truncated)" : b;
    }
}
