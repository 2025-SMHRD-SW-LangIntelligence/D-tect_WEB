package com.smhrd.dtect.service.model;

import com.smhrd.dtect.config.ModelProperties;
import com.smhrd.dtect.dto.ModelMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HttpModelGateway implements ModelGateway {

    private final WebClient modelWebClient;
    private final ModelProperties props;

    @Override
    public List<ModelMessage> predict(byte[] imageBytes, String filename, Long analId) throws Exception {
        if (props.getBaseUrl() == null || props.getBaseUrl().isBlank()) {
            return List.of();
        }

        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("file", imageBytes)
                .filename(filename != null ? filename : "frame.png")
                .contentType(MediaType.IMAGE_PNG);

        return modelWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(props.getPredictPath())
                        .queryParam("analId", analId)
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(mb.build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ModelMessage>>() {})
                .map(list -> list)
                .onErrorReturn(List.of())
                .block();
    }
}
