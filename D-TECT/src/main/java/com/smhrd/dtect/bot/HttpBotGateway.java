package com.smhrd.dtect.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.smhrd.dtect.dto.BotMessageRequest;
import com.smhrd.dtect.dto.BotMessageResponse;

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
        return client.post()
            .uri("/chat") // 🔁 외부 챗봇 서버의 실제 경로로 맞춰줘
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(BotMessageResponse.class);
    }
}
