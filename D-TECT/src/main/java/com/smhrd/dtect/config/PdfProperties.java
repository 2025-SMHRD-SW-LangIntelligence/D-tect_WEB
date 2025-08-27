package com.smhrd.dtect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pdf")
public class PdfProperties {
    /**
     * PDF 생성/저장용 웹훅 베이스 URL (예: https://n8n.example.com/webhook/D-tect_analyzePdf)
     * 비어있으면 스텁(호출하지 않음)
     */
    private String webhookUrl;

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
}
