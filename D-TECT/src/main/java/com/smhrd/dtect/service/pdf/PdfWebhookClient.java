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
import java.util.Map;
import java.util.Objects;

//com/smhrd/dtect/service/pdf/PdfWebhookClient.java
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfWebhookClient {

 private final PdfProperties pdfProps;
 private final WebClient webClient = WebClient.builder().build(); // 주입형이어도 OK

 public boolean dispatchCountsWithAnalId(
         Long analId,
         String username,
         String sid,
         Map<FieldName, Integer> typeCounts,
         AnalRate analRate,
         Instant startedAt,
         Instant endedAt
 ) {
	 final String url = pdfProps.getWebhookUrl();
	 log.info("[PdfWebhook] resolved webhookUrl={}", url);
	 if (url == null || url.isBlank()) {
	     log.error("[PdfWebhook] webhookUrl NOT configured (app.pdf.webhook-url). Skip dispatch. analId={} sid={}", analId, sid);
	     return false;
	 }

     final String callbackUrl = buildCallbackUrl(analId);
     final Map<String,Integer> counts = toStringKeyMap(typeCounts);
     final int sum = counts.values().stream().mapToInt(Integer::intValue).sum();

     Map<String,Object> body = new LinkedHashMap<>();
     if (analId != null) body.put("analId", analId);
     body.put("username", username == null ? "" : username);
     body.put("sid", sid == null ? "" : sid);
     body.put("period", Map.of(
             "startedAt", startedAt != null ? startedAt.toString() : null,
             "endedAt",   endedAt   != null ? endedAt.toString()   : null
     ));
     body.put("typeCounts", counts);
     body.put("analRate", (analRate != null ? analRate : AnalRate.NORMAL).name());
     if (callbackUrl != null) body.put("callbackUrl", callbackUrl);

     log.info("[PdfWebhook] → POST {} | analId={} sid={} sum={} callbackUrl={}",
             url, analId, sid, sum, callbackUrl);
     log.info("[PdfWebhook] Request body: {}", body);
     Boolean ok = webClient.post()
             .uri(url)
             .contentType(MediaType.APPLICATION_JSON)
             .bodyValue(body)
             .exchangeToMono(resp -> resp.bodyToMono(String.class).defaultIfEmpty("")
                     .map(b -> {
                         if (resp.statusCode().is2xxSuccessful()) {
                             log.info("[PdfWebhook] ← {} OK analId={} sid={} bodyLen={}",
                                     resp.statusCode(), analId, sid, b.length());
                             return true;
                         } else {
                             log.error("[PdfWebhook] ← {} FAIL analId={} sid={} body={}",
                                     resp.statusCode(), analId, sid, b);
                             return false;
                         }
                     }))
             .onErrorResume(e -> {
                 log.error("[PdfWebhook] EXC analId={} sid={} err={}", analId, sid, e.toString());
                 return reactor.core.publisher.Mono.just(false);
             })
             .block();
     
     return Boolean.TRUE.equals(ok);
 }

 public boolean dispatchCounts(String username, String sid,
                               Map<FieldName,Integer> typeCounts, AnalRate rate,
                               Instant startedAt, Instant endedAt) {
     // analId 없이도 호출 가능(레거시 호환)
     return dispatchCountsWithAnalId(null, username, sid, typeCounts, rate, startedAt, endedAt);
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
}

