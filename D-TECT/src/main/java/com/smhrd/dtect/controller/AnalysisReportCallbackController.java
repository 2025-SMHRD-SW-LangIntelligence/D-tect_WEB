// com/smhrd/dtect/controller/AnalysisReportCallbackController.java
package com.smhrd.dtect.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smhrd.dtect.dto.PdfCallbackResponse;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.service.AnalysisResultService;   // ✅ 추가
import com.smhrd.dtect.storage.NcpS3ReportStorageWriter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;            // ✅ 추가
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/analysis")
public class AnalysisReportCallbackController {

    private final AnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper;
    private final NcpS3ReportStorageWriter storageWriter;
    private final AnalysisResultService analysisResultService; // ✅ 추가

    /** n8n → PDF 완료 콜백 */
    @PostMapping("/{analId}/pdf-callback")
    @Transactional
    public ResponseEntity<PdfCallbackResponse> pdfCallback(
            @PathVariable("analId") Long analId,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "payload", required = false) String payloadJson
    ) {
        try {
            Analysis a = analysisRepository.findById(analId)
                    .orElseThrow(() -> new IllegalArgumentException("analysis not found: " + analId));

            String objectKey = null;

            // payloadJson 안에서 objectKey 추출 (n8n이 직접 업로드해줬을 때)
            if (payloadJson != null && !payloadJson.isBlank()) {
                try {
                    JsonNode node = objectMapper.readTree(payloadJson);
                    if (node.hasNonNull("objectKey")) {
                        objectKey = node.get("objectKey").asText();
                    }
                } catch (Exception e) {
                    log.warn("[PdfCallback] payloadJson parse fail analId={} err={}", analId, e.toString());
                }
            }

            // ✅ 파일만 온 경우: 우리가 S3에 업로드하며 파일명 = "name의 결과 보고서.pdf"
            if (objectKey == null && file != null) {
                String name = analysisResultService.getNameForAnalId(analId);
                String safe = sanitizeName(name);
                String datePath = LocalDate.now().toString();
                String filename = safe + "의 결과 보고서.pdf";
                String key = "reports/" + datePath + "/analysis-" + analId + "-" + filename;

                objectKey = storageWriter.upload(key, file.getBytes(), file.getContentType());
            }

            if (objectKey == null || objectKey.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new PdfCallbackResponse(analId, "objectKey missing"));
            }

            // DB에는 objectKey만 저장
            a.setReportUrl(objectKey);
            if (a.getFinishedAt() == null) {
                a.setFinishedAt(Timestamp.from(Instant.now()));
            }
            analysisRepository.save(a);

            log.info("[PdfCallback] 분석#{} 저장 완료 objectKey={}", analId, objectKey);
            return ResponseEntity.ok(new PdfCallbackResponse(analId, "OK"));

        } catch (Exception e) {
            log.error("[PdfCallback] 실패 analId={} err={}", analId, e.toString(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PdfCallbackResponse(analId, "FAIL: " + e.getMessage()));
        }
    }

    /** 결과 페이지에서 호출 → presigned URL + name 반환 */
    @GetMapping("/{analId}/report")
    public ResponseEntity<?> getReportUrl(@PathVariable("analId") Long analId) {
        Optional<Analysis> opt = analysisRepository.findById(analId);
        if (opt.isEmpty() || opt.get().getReportUrl() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("analId", analId, "reportUrl", null));
        }

        // DB에는 objectKey만 저장됨
        String objectKey = opt.get().getReportUrl();

        // presigned URL 생성 (7일)
        String presignedUrl = storageWriter.generatePresignedUrl(objectKey);

        // ✅ 프론트 파일명용 name 포함
        String name = analysisResultService.getNameForAnalId(analId);

        log.info("[ReportAPI] 분석#{} objectKey={} → presignedUrl={} name={}", analId, objectKey, presignedUrl, name);

        return ResponseEntity.ok(Map.of(
                "analId", analId,
                "reportUrl", presignedUrl,
                "expiresInDays", 7,
                "name", name
        ));
    }

    // ===== 유틸 =====
    private static String sanitizeName(String raw) {
        if (raw == null || raw.isBlank()) return "사용자";
        // S3 키에 문제될 수 있는 문자 최소 정리(경로 구분자 등)
        String s = raw.replaceAll("[/\\\\:*?\"<>|#%&+]", " ").trim();
        // 공백 압축
        return s.replaceAll("\\s{2,}", " ");
    }
}
