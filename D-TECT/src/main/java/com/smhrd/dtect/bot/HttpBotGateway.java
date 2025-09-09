package com.smhrd.dtect.bot;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.smhrd.dtect.dto.bot.BotMessageRequest;
import com.smhrd.dtect.dto.bot.BotMessageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HttpBotGateway implements BotGateway {

    private final RestClient.Builder builder;

    @Value("${app.bot.base-url}")
    private String baseUrl;

    @Override
    public BotMessageResponse ask(BotMessageRequest request) {
        RestClient client = builder.baseUrl(baseUrl).build();

        // ✅ 우리 내부 DTO(req: sessionId, text, context) → 챗봇 서버 요구 스키마로 변환
        Map<String, Object> remotePayload = new HashMap<>();
        remotePayload.put("message", request.text());             // ← 핵심: text → message
        remotePayload.put("session_id", request.sessionId());     // ← snake_case가 보통 맞습니다
        remotePayload.put("context", request.context());

        // // 만약 서버가 role/content 구조를 요구한다면 이렇게:
        // remotePayload.put("message", Map.of("role", "user", "content", request.text()));

        return client.post()
            .uri("/chat") // 실제 엔드포인트에 맞추세요
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(remotePayload)
            .retrieve()
            .body(BotMessageResponse.class);
    }

}
