package com.smhrd.dtect.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(PdfProperties.class)
public class PdfClientConfig {
    @Bean
    public WebClient pdfWebClient() {
        return WebClient.builder().build();
    }
}
