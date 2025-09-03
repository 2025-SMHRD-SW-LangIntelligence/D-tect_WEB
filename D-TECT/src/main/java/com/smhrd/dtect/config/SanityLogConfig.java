package com.smhrd.dtect.config;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SanityLogConfig {

    private final ModelProperties modelProps;
    private final PdfProperties pdfProps;

    @PostConstruct
    public void logProps() {
        log.info("[Props] model baseUrl={}, predictPath={}, connectTimeoutMs={}, readTimeoutMs={}",
                modelProps.getBaseUrl(), modelProps.getPredictPath(),
                modelProps.getConnectTimeoutMs(), modelProps.getReadTimeoutMs());
        log.info("[Props] pdf webhookUrl={}", pdfProps.getWebhookUrl());
    }
}
