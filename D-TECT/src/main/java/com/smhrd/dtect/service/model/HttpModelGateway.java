package com.smhrd.dtect.service.model;

import com.smhrd.dtect.config.ModelProperties;
import com.smhrd.dtect.dto.ModelResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HttpModelGateway implements ModelGateway {

    private final WebClient modelWebClient;
    private final ModelProperties props;

    @Override
    public List<ModelResultDto> predict(byte[] imageBytes, String filename) throws Exception {
        // 🔴 모델 URL이 아직 비어있으면 "스텁 모드": 빈 결과 반환
        if (props.getBaseUrl() == null || props.getBaseUrl().isBlank()) {
            return List.of();
        }

        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("file", imageBytes)
          .filename(filename != null ? filename : "frame.jpg")
          .contentType(MediaType.IMAGE_JPEG); // FE에서 JPEG 전송 권장

        return modelWebClient.post()
                .uri(props.getPredictPath())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(mb.build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ModelResultDto>>() {})
                .block();
    }
}
