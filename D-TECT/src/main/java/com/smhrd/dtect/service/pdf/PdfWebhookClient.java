package com.smhrd.dtect.service.pdf;

import com.smhrd.dtect.config.PdfProperties;
import com.smhrd.dtect.dto.ModelResultDto;
import com.smhrd.dtect.entity.AnalRate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
public class PdfWebhookClient {

    private final WebClient pdfWebClient;
    private final PdfProperties props;

    /**
     * n8n의 JSON→HTML 노드가 const data = $input.first().json.results 로 읽으므로
     * 바디 최상위에 results 객체를 넣어 보냅니다.
     */
    public boolean dispatchJson(Long userId,
                                String sid,
                                List<ModelResultDto> items,
                                AnalRate rate,
                                Long analIdOrNull,
                                String reportPathOrNull) {
        if (props.getWebhookUrl() == null || props.getWebhookUrl().isBlank()) {
            // 웹훅 URL 비어 있으면 스텁: 성공 처리
            return true;
        }

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("anal_idx", String.valueOf(analIdOrNull != null ? analIdOrNull : sid)); // 템플릿 호환 위해 문자열로
        results.put("anal_rate", rate != null ? rate.name() : "NORMAL");
        results.put("anal_result", buildTextSummary(items));
        results.put("reportPath", Optional.ofNullable(reportPathOrNull).orElse(""));
        results.put("sid", sid);
        results.put("userId", userId);
        results.put("createdAt", Instant.now().toString());
        results.put("items", (items == null ? List.of() : items));

        Map<String, Object> body = Map.of("results", results);

        try {
            pdfWebClient.post()
                    .uri(props.getWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 간단 요약문 생성: 상위 20개 항목을 한 줄씩 텍스트로 만든다(인쇄 친화)
    private static String buildTextSummary(List<ModelResultDto> items) {
        if (items == null || items.isEmpty()) return "감지된 유의미한 항목이 없습니다.";
        final int TOP = 20;
        StringBuilder sb = new StringBuilder(8_000);
        items.stream()
                .sorted(Comparator.comparingDouble(PdfWebhookClient::score).reversed())
                .limit(TOP)
                .forEach(r -> {
                    String label = (r.classification() != null) ? r.classification().label() : "UNKNOWN";
                    int count    = (r.classification() != null) ? r.classification().count() : 0;
                    String user  = nvl(r.user(), "—");
                    String text  = nvl(r.text(), "—").replaceAll("\\s+", " ").trim();
                    String score = fmt(r.score());
                    sb.append("• [").append(label).append("] score=").append(score)
                      .append(", count=").append(count)
                      .append(", user=").append(user)
                      .append(" — ").append(text)
                      .append("\n");
                });
        return sb.toString();
    }

    private static String nvl(String s, String d){ return (s==null || s.isBlank()) ? d : s; }
    private static double score(ModelResultDto r){
        try { return r != null && r.score() != null ? Double.parseDouble(r.score()) : 0d; }
        catch(Exception e){ return 0d; }
    }
    private static String fmt(String s){
        try { return String.format(Locale.US, "%.2f", Double.parseDouble(s)); }
        catch(Exception e){ return "0.00"; }
    }
}
