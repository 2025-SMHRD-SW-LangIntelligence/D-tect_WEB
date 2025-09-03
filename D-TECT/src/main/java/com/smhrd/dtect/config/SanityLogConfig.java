package com.smhrd.dtect.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SanityLogConfig {

    private final PdfProperties pdfProps;
    private final StorageProperties storageProps;

    @PostConstruct
    public void logProps() {
        log.info("[Props] pdf webhookUrl={}", pdfProps.getWebhookUrl());
        log.info("[Props] storage provider={} publicBaseUrl={}",
                storageProps.getProvider(), storageProps.getPublicBaseUrl());
    }
}
