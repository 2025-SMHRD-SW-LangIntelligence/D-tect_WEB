package com.smhrd.dtect.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import io.netty.channel.ChannelOption;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final ModelProperties modelProps; // ✅ app.model.* 값 주입

    @Bean(name = "modelServerWebClient")
    public WebClient modelWebClient() {
        // 타임아웃 설정
        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofMillis(modelProps.getReadTimeoutMs()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, modelProps.getConnectTimeoutMs());

        return WebClient.builder()
                .baseUrl(modelProps.getBaseUrl()) // ✅ app.model.base-url
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(http))
                .build();
    }
}
