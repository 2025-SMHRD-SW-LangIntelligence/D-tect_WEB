// src/main/java/com/smhrd/dtect/config/PdfProperties.java
package com.smhrd.dtect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "app.pdf")
public class PdfProperties {
    /** n8n 웹훅 절대 URL (필수) */
    private String webhookUrl;

    /** 콜백 URL 템플릿(선택): 예) https://app.example.com/api/analysis/{analId}/pdf-callback */
    private String callbackUrlTemplate;

    /** 우리 서버 퍼블릭 베이스 URL(선택): 템플릿 없을 때 fallback로 사용 */
    private String publicBaseUrl;

    /** 연결/응답 타임아웃(선택) */
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 60000;

    // getters/setters
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public String getCallbackUrlTemplate() { return callbackUrlTemplate; }
    public void setCallbackUrlTemplate(String callbackUrlTemplate) { this.callbackUrlTemplate = callbackUrlTemplate; }
    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    
    @PostConstruct
    public void debugInit() {
        System.out.println("### PdfProperties.webhookUrl=" + webhookUrl);
    }
    
}
