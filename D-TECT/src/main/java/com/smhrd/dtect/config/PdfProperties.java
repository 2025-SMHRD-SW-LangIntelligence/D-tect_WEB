// src/main/java/com/smhrd/dtect/config/PdfProperties.java
package com.smhrd.dtect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.pdf")
public class PdfProperties {
    private String webhookUrl;
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
}
