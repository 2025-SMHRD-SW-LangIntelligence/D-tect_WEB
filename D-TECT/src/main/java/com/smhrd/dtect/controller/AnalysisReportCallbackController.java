package com.smhrd.dtect.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.smhrd.dtect.dto.PdfCallbackResponse;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.repository.UserRepository;
import com.smhrd.dtect.service.AnalysisResultService;
import com.smhrd.dtect.storage.ReportStorageWriter;
import com.smhrd.dtect.support.AnalRateSafe;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/analysis")
public class AnalysisReportCallbackController {

    private final AnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final AnalysisResultService analysisResultService;
    private final ReportStorageWriter writer; // NCP 업로더 래퍼
    private final com.smhrd.dtect.config.StorageProperties storageProps;

    /** (A) JSON 케이스: { sid, userId, analRate, analResult, reportUrl | pdf[0].url } */
    @PostMapping(value = "/pdf-callback", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<PdfCallbackResponse> pdfReadyJson(@RequestBody JsonNode root) {
        JsonNode node = root.hasNonNull("results") ? root.get("results") : root;

        String sid = text(node, "sid");
        Long userId = longOrNull(node, "userId");
        if (userId == null && sid != null) userId = analysisResultService.getUserIdForSid(sid);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new PdfCallbackResponse(null, "cannot resolve userId"));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new PdfCallbackResponse(null, "user not found: " + userId));

        String analRateStr = firstNonBlank(text(node, "analRate"), text(node, "anal_rate"));
        AnalRate rate = AnalRateSafe.fromNullable(analRateStr);

        JsonNode analResultNode = node.has("analResult") ? node.get("analResult")
                : node.has("anal_result") ? node.get("anal_result") : null;
        String analResultJson = (analResultNode == null) ? "" : analResultNode.toString();

        String reportUrl = firstNonBlank(text(node, "reportUrl"), text(node, "report_path"), text(node, "reportPath"), firstPdfUrl(node));
        if (isBlank(reportUrl)) return ResponseEntity.badRequest().body(new PdfCallbackResponse(null, "missing reportUrl (or pdf[0].url)"));

        Analysis a = new Analysis();
        a.setUser(user);
        a.setAnalResult(analResultJson);
        a.setAnalRate(rate);
        a.setReportUrl(reportUrl);
        a.setCreatedAt(Timestamp.from(Instant.now()));

        Analysis saved = analysisRepository.save(a);
        return ResponseEntity.ok(new PdfCallbackResponse(saved.getAnalIdx(), "OK"));
    }

    /** (B) Multipart 케이스: 바이너리 PDF 업로드 */
    @PostMapping(value = "/pdf-callback", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<PdfCallbackResponse> pdfReadyMultipart(
            @RequestParam(required = false) String sid,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "NORMAL") String analRate,
            @RequestParam(required = false) String analResult,
            @RequestPart("file") MultipartFile file
    ) throws Exception {

        if (userId == null && sid != null) userId = analysisResultService.getUserIdForSid(sid);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new PdfCallbackResponse(null, "userId missing"));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new PdfCallbackResponse(null, "user not found: " + userId));

        String contentType = Optional.ofNullable(file.getContentType()).orElse("application/pdf");
        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("report.pdf");
        String owner = (sid == null || sid.isBlank()) ? String.valueOf(userId) : sid;

        String key = writer.uploadAutoName(owner, originalName, file.getBytes(), contentType);
        String reportUrl = buildPublicUrl(key);
        if (isBlank(reportUrl)) return ResponseEntity.badRequest().body(new PdfCallbackResponse(null, "failed to build reportUrl"));

        Analysis a = new Analysis();
        a.setUser(user);
        a.setAnalRate(AnalRateSafe.fromNullable(analRate));
        a.setAnalResult(Optional.ofNullable(analResult).orElse(""));
        a.setReportUrl(reportUrl);
        a.setCreatedAt(Timestamp.from(Instant.now()));

        Analysis saved = analysisRepository.save(a);
        return ResponseEntity.ok(new PdfCallbackResponse(saved.getAnalIdx(), "OK"));
    }

    // ===== helpers =====

    private String buildPublicUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return null;
        String provider = String.valueOf(storageProps.getProvider()).toLowerCase(Locale.ROOT);
        if ("ncp".equals(provider)) {
            String base = trimRightSlash(storageProps.getPublicBaseUrl());
            return (base != null) ? base + "/" + objectKey : null;
        }
        if ("local".equals(provider)) return "/uploads/" + objectKey;
        return null;
    }

    private static String trimRightSlash(String s) { return (s == null || s.isBlank()) ? null : s.replaceAll("/+$", ""); }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String text(JsonNode n, String key) { if (n == null || key == null) return null; JsonNode v = n.get(key); return (v == null || v.isNull()) ? null : (v.isTextual() ? v.asText() : v.toString()); }
    private static Long longOrNull(JsonNode n, String key) {
        if (n == null) return null; JsonNode v = n.get(key); if (v == null || v.isNull()) return null;
        try { if (v.isNumber()) return v.longValue(); if (v.isTextual()) return Long.parseLong(v.asText().trim()); } catch (Exception ignore) {}
        return null;
    }
    private static String firstNonBlank(String... arr) { if (arr == null) return null; for (String s : arr) if (!isBlank(s)) return s; return null; }
    /** results.pdf[0].url 추출 */
    private static String firstPdfUrl(JsonNode n) {
        if (n == null) return null;
        JsonNode pdf = n.get("pdf");
        if (pdf != null && pdf.isArray() && pdf.size() > 0) {
            JsonNode first = pdf.get(0);
            String url = first != null ? text(first, "url") : null;
            if (!isBlank(url)) return url;
        }
        return null;
    }
}
