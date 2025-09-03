package com.smhrd.dtect.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class WebClientConfig {

    private final ModelProperties modelProps;

    @Bean(name = "modelServerWebClient")
    public WebClient modelServerWebClient(
            @Value("${app.model.max-in-memory-mb:10}") int maxInMemoryMb
    ) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs()
                        .maxInMemorySize(Math.max(1, maxInMemoryMb) * 1024 * 1024))
                .build();

        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofMillis(modelProps.getReadTimeoutMs()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, modelProps.getConnectTimeoutMs());

        return WebClient.builder()
                .baseUrl(modelProps.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(http))
                .build();
    }
}
