package com.smhrd.dtect.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpConfig {

    @Bean
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            @Value("${http.client.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${http.client.read-timeout-ms:60000}") long readTimeoutMs,
            @Value("${http.client.user-agent:D-TECT/1.0 (+payment)}") String userAgent,
            @Value("${http.client.force-user-agent:false}") boolean forceUserAgent
    ) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .additionalInterceptors((req, body, exec) -> {
                    if (forceUserAgent || !req.getHeaders().containsKey(HttpHeaders.USER_AGENT)) {
                        req.getHeaders().set(HttpHeaders.USER_AGENT, userAgent);
                    }
                    return exec.execute(req, body);
                })
                .build();
    }
}

