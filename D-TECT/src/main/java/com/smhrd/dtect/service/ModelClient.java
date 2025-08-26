package com.smhrd.dtect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.smhrd.dtect.dto.LabelCount;
import com.smhrd.dtect.dto.ModelRequest;
import com.smhrd.dtect.dto.ModelResponse;

import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * 모델 서버 호출용 WebClient 래퍼.
 * - 항상 classification 배열로 전송
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModelClient {

    private final WebClient modelWebClient;

    @Value("${model.endpoint.infer:/infer}")
    private String inferPath;

    public Mono<ModelResponse> infer(String user, String text, String score, List<LabelCount> classificationOpt) {

        List<LabelCount> classification = (classificationOpt != null)
                ? classificationOpt : Collections.emptyList();

        ModelRequest payload = ModelRequest.builder()
                .user(user)
                .text(text)
                .score(score)                 // 필요 없으면 null 허용
                .classification(classification)
                .build();

        log.debug("[ModelClient] POST {} body.user={} classCount={}", inferPath, user, classification.size());

        return modelWebClient.post()
                .uri(inferPath)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(ModelResponse.class);
    }
}
