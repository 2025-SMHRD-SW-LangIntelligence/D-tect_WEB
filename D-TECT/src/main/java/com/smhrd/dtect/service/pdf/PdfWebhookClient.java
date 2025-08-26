package com.smhrd.dtect.service.pdf;

import com.smhrd.dtect.config.PdfProperties;
import com.smhrd.dtect.dto.LabelCount;
import com.smhrd.dtect.dto.ModelMessage;
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
                                List<ModelMessage> items,
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
    private static String buildTextSummary(List<ModelMessage> items) {
        if (items == null || items.isEmpty()) return "감지된 유의미한 항목이 없습니다.";
        final int TOP = 20;
        StringBuilder sb = new StringBuilder(8_000);

        items.stream()
        	.sorted(Comparator.comparingDouble(m -> parseScore(((ModelMessage) m).getScore())).reversed())
            .limit(TOP)
            .forEach(m -> {
                LabelCount top = topLabel(m.getClassification()); // 새 스키마: 배열 중 최댓값 1개 선택
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

    // classification 배열에서 count가 가장 큰 라벨 1개 선택 (없으면 null)
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
}
