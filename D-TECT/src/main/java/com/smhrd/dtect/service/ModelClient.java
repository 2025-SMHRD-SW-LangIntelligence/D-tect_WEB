package com.smhrd.dtect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.smhrd.dtect.config.ModelProperties;
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

    private final WebClient modelWebClient;   // WebClientConfig 에서 생성
    private final ModelProperties modelProps; // ✅ 주입

    public Mono<ModelResponse> infer(String user, String text, String score, List<LabelCount> classificationOpt) {

        // baseUrl 비어 있으면 스텁으로 동작
        if (modelProps.getBaseUrl() == null || modelProps.getBaseUrl().isBlank()) {
            log.warn("[ModelClient] baseUrl is blank. Returning stub response.");
            ModelResponse stub = ModelResponse.builder()
                    .user(user)
                    .text(text)
                    .score(score)
                    .classification(classificationOpt != null ? classificationOpt : Collections.emptyList())
                    .build();
            return Mono.just(stub);
        }

        List<LabelCount> classification = (classificationOpt != null) ? classificationOpt : Collections.emptyList();
        String path = modelProps.getPredictPath(); // ✅ app.model.predict-path (기본 /predict)

        return modelWebClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ModelRequest.builder()
                        .user(user)
                        .text(text)
                        .score(score)
                        .classification(classification)
                        .build())
                .retrieve()
                .bodyToMono(ModelResponse.class);
    }
}
