package com.smhrd.dtect.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class PdfDownloader {

    private final WebClient webClient = WebClient.builder().build();

    public byte[] downloadPdf(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("download url is blank");
        }
        return webClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_PDF)
                .retrieve()
                .bodyToMono(byte[].class)
                .blockOptional()
                .orElseThrow(() -> new IllegalStateException("download failed: " + url));
    }
}
