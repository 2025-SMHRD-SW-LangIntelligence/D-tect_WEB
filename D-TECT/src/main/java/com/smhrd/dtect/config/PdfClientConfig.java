// src/main/java/com/smhrd/dtect/config/PdfClientConfig.java
package com.smhrd.dtect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PdfClientConfig {

    @Bean
    public WebClient pdfWebClient(WebClient.Builder builder) {
        return builder.build();
    }
}
