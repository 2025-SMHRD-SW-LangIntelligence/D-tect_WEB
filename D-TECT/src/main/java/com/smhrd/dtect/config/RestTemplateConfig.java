// src/main/java/com/smhrd/dtect/config/RestTemplateConfig.java
package com.smhrd.dtect.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @Primary // 혹시 다른 RestTemplate 빈이 생겨도 기본값으로 사용
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            @Value("${http.client.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${http.client.read-timeout-ms:60000}") long readTimeoutMs
    ) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .additionalInterceptors((req, body, exec) -> {
                    req.getHeaders().set(HttpHeaders.USER_AGENT, "D-TECT/1.0 (+payment)");
                    return exec.execute(req, body);
                })
                .build();
    }
}
