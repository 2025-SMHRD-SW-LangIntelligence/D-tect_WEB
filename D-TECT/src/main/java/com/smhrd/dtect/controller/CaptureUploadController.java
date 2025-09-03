package com.smhrd.dtect.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/capture")
@RequiredArgsConstructor
public class CaptureUploadController {

    private final WebClient modelWebClient;

    // ModelProperties 대체: application.yml 의 model.predict-path 사용
    @Value("${model.predict-path:/predict}")
    private String predictPath;

    @PostMapping(value = "/frame", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> frame(
            @RequestParam("analId") Long analId,
            @RequestParam("file") MultipartFile file
    ) {

        if (analId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "analId is required"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "file is required"));
        }

        final String filename = (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
                ? file.getOriginalFilename()
                : "frame";
        final MediaType partContentType = parseOrDefault(file.getContentType(), MediaType.APPLICATION_OCTET_STREAM);

        ByteArrayResource resource;
        try {
            byte[] bytes = file.getBytes();
            resource = new NamedByteArrayResource(bytes, filename);
        } catch (Exception e) {
            log.warn("Failed to read uploaded file: {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "failed to read file bytes"));
        }

        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("file", resource)
          .filename(filename)
          .contentType(partContentType);

        Map<String, Object> resp;
        try {
            resp = modelWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(Objects.requireNonNullElse(predictPath, "/predict"))
                            .queryParam("analId", analId)
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(mb.build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

        } catch (WebClientResponseException wcre) {
            log.warn("Model server error: status={}, body={}", wcre.getRawStatusCode(), wcre.getResponseBodyAsString());
            resp = null;
        } catch (Exception e) {
            log.warn("Model server call failed: {}", e.toString());
            resp = null;
        }

        if (resp == null) {
            resp = Map.of("flagged", false, "labels", List.of());
        } else {
            resp = ensureKeys(resp, "flagged", false, "labels", List.of());
        }

        return ResponseEntity.ok(resp);
    }

    private static MediaType parseOrDefault(String ct, MediaType def) {
        try {
            return (ct == null || ct.isBlank()) ? def : MediaType.parseMediaType(ct);
        } catch (Exception ignored) {
            return def;
        }
    }

    private static Map<String, Object> ensureKeys(Map<String, Object> src,
                                                  String k1, Object v1Default,
                                                  String k2, Object v2Default) {
        Object v1 = src.getOrDefault(k1, v1Default);
        Object v2 = src.getOrDefault(k2, v2Default);
        return Map.of(k1, v1, k2, v2);
    }

    /** filename 보존용 */
    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;
        public NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }
        @Override public String getFilename() { return filename; }
    }
}
