package com.smhrd.dtect.controller;

import com.smhrd.dtect.config.ModelProperties;
import com.smhrd.dtect.dto.FrameAnalyzeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/capture")
@RequiredArgsConstructor
public class CaptureUploadController {

    private final WebClient modelWebClient;
    private final ModelProperties props;

    @PostMapping(value = "/frame", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> frame(
            @RequestParam("analId") Long analId,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        byte[] bytes = file.getBytes();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "frame.png";

        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("file", bytes)
                .filename(filename)
                .contentType(MediaType.IMAGE_PNG);

        // 모델 서버로 프록시 (flagged/labels json 기대)
        Map<String, Object> resp = modelWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(props.getPredictPath())
                        .queryParam("analId", analId)
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(mb.build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (resp == null) {
            resp = Map.of("flagged", false, "labels", List.of());
        }
        return ResponseEntity.ok(resp);
    }
}
