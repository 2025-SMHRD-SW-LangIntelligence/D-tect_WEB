// com/smhrd/dtect/service/pdf/PdfWebhookClient.java
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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfWebhookClient {

    private final PdfProperties pdfProps;
    private final WebClient webClient = WebClient.builder().build();

    /** username + name 둘 다 전송하도록 시그니처 변경 */
    public boolean dispatchCountsWithAnalId(
            Long analId,
            String username,
            String name,
            String sid,
            Map<FieldName, Integer> typeCounts,
            AnalRate analRate,
            Instant startedAt,
            Instant endedAt
    ) {
        final String url = pdfProps.getWebhookUrl();
        if (url == null || url.isBlank()) {
            return false;
        }

        final String callbackUrl = buildCallbackUrl(analId);
        final Map<String,Integer> counts = toStringKeyMap(typeCounts);
        final int sum = counts.values().stream().mapToInt(Integer::intValue).sum();

        Map<String,Object> body = new LinkedHashMap<>();
        if (analId != null) body.put("analId", analId);
        body.put("username", username == null ? "" : username);
        body.put("name",     name == null ? "사용자" : name);
        body.put("sid", sid == null ? "" : sid);
        body.put("period", Map.of(
                "startedAt", startedAt != null ? startedAt.toString() : null,
                "endedAt",   endedAt   != null ? endedAt.toString()   : null
        ));
        body.put("typeCounts", counts);
        body.put("analRate", (analRate != null ? analRate : AnalRate.NORMAL).name());
        if (callbackUrl != null) body.put("callbackUrl", callbackUrl);

        Boolean ok = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(resp -> resp.bodyToMono(String.class).defaultIfEmpty("")
                        .map(b -> {
                            if (resp.statusCode().is2xxSuccessful()) {
                                return true;
                            } else {
                                return false;
                            }
                        }))
                .onErrorResume(e -> {
                    return reactor.core.publisher.Mono.just(false);
                })
                .block();

        return Boolean.TRUE.equals(ok);
    }

    /** 레거시 호환(analId 없이도 가능) — 필요시 유지 */
    public boolean dispatchCounts(String username, String name,
                                  String sid, Map<FieldName,Integer> typeCounts, AnalRate rate,
                                  Instant startedAt, Instant endedAt) {
        return dispatchCountsWithAnalId(null, username, name, sid, typeCounts, rate, startedAt, endedAt);
    }

    private String buildCallbackUrl(Long analId) {
        if (analId == null) return null;
        String tpl = pdfProps.getCallbackUrlTemplate();
        if (tpl != null && !tpl.isBlank()) return tpl.replace("{analId}", String.valueOf(analId));
        String base = pdfProps.getPublicBaseUrl();
        if (base != null && !base.isBlank()) {
            String b = base.replaceAll("/+$", "");
            return b + "/api/analysis/" + analId + "/pdf-callback";
        }
        return null;
    }

    private static Map<String,Integer> toStringKeyMap(Map<FieldName,Integer> src) {
        Map<String,Integer> out = new LinkedHashMap<>();
        if (src != null) src.forEach((k,v)-> out.put(k.name(), v==null?0:v));
        return out;
    }
    
    private static FieldName toFieldNameFlexible(String raw) {
		if (raw == null)
			return null;
		String s = raw.trim().toUpperCase(Locale.ROOT);
		switch (s) { // 별칭 보정
		case "HARASSMENT" -> s = "BULLYING";
		case "BLACKMAIL" -> s = "CHANTAGE";
		case "VIOLENT" -> s = "VIOLENCE";
		}
		try {
			return FieldName.valueOf(s);
		} catch (Exception ignore) {
			return null;
		}
    }
	
}
