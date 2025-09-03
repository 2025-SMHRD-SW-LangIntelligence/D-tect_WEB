package com.smhrd.dtect.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smhrd.dtect.dto.PdfCallbackResponse;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.storage.ReportUrlBuilder;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/analysis")
public class AnalysisReportCallbackController {

    private final AnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper;
    private final ReportUrlBuilder reportUrlBuilder;
    private final AnalysisSseController analysisSseController; // 🔹 SSE 알림 주입

    /**
     * n8n → PDF 완료 콜백 수신
     */
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

            String reportUrl = null;

            // 1) payloadJson 안에 reportUrl 있으면 우선 사용
            if (payloadJson != null && !payloadJson.isBlank()) {
                try {
                    JsonNode node = objectMapper.readTree(payloadJson);
                    if (node.hasNonNull("reportUrl")) {
                        reportUrl = node.get("reportUrl").asText();
                    }
                } catch (Exception e) {
                    log.warn("[PdfCallback] payloadJson 파싱 실패 analId={} err={}", analId, e.toString());
                }
            }

            // 2) 파일만 왔고 reportUrl이 없으면 → 직접 URL 조립
            if (reportUrl == null && file != null) {
                String objectKey = String.format("reports/%s/%d-report.pdf",
                        Instant.now().toString().substring(0, 7), analId);
                reportUrl = reportUrlBuilder.toPublicUrl(objectKey);
            }

            if (reportUrl == null || reportUrl.isBlank()) {
                log.error("[PdfCallback] reportUrl 누락 analId={}", analId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new PdfCallbackResponse(analId, "reportUrl not provided"));
            }

            // 3) DB 업데이트
            a.setReportUrl(reportUrl);
            if (a.getFinishedAt() == null) {
                a.setFinishedAt(Timestamp.from(Instant.now()));
            }
            analysisRepository.save(a);

            log.info("[PdfCallback] 분석#{} → reportUrl={} 저장 완료", analId, reportUrl);

            // 4) SSE 알림 전송 (프론트가 실시간으로 버튼 활성화 가능)
            analysisSseController.notifyReady(analId, reportUrl);

            return ResponseEntity.ok(new PdfCallbackResponse(analId, "OK"));

        } catch (Exception e) {
            log.error("[PdfCallback] 실패 analId={} err={}", analId, e.toString(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PdfCallbackResponse(analId, "FAIL: " + e.getMessage()));
        }
    }
}
