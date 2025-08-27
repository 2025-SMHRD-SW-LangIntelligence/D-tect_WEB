package com.smhrd.dtect.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.smhrd.dtect.dto.PdfCallbackResponse;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.repository.MemberRepository;
import com.smhrd.dtect.service.AnalysisResultService;
import com.smhrd.dtect.support.AnalRateSafe;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/analysis")
public class AnalysisPdfCallbackController {

    private final AnalysisRepository analysisRepository;
    private final MemberRepository memberRepository;
    private final AnalysisResultService analysisResultService;

    @PostMapping(
        value = "/pdf-callback",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional
    public ResponseEntity<PdfCallbackResponse> pdfReady(@RequestBody JsonNode root) {

        JsonNode node = root.hasNonNull("results") ? root.get("results") : root;

        String sid = text(node, "sid");

        // 1) userId(Long) 우선, 없거나 문자열/잘못된 값이면 sid로 세션에서 복원
        Long userId = longOrNull(node, "userId");
        if (userId == null && sid != null) userId = analysisResultService.getUserIdForSid(sid);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new PdfCallbackResponse(null, "unauthorized: cannot resolve userId"));
        }

        // 2) Member 조회
        Member member = memberRepository.findById(userId).orElse(null);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new PdfCallbackResponse(null, "member not found: " + userId));
        }

        // 3) 등급/결과
        String analRateStr = firstNonBlank(text(node, "analRate"), text(node, "anal_rate"));
        AnalRate rate = AnalRateSafe.fromNullable(analRateStr);

        JsonNode analResultNode = node.has("analResult") ? node.get("analResult")
                : node.has("anal_result") ? node.get("anal_result") : null;
        String analResultJson = (analResultNode == null) ? "null" : analResultNode.toString();

        // 4) reportUrl
        String reportUrl = firstNonBlank(
                text(node, "reportUrl"),
                text(node, "report_path"),
                text(node, "reportPath"),
                firstPdfUrl(node)
        );
        if (isBlank(reportUrl)) {
            return ResponseEntity.badRequest()
                    .body(new PdfCallbackResponse(null, "missing reportUrl (or pdf[0].url)"));
        }

        // 5) 저장
        Analysis a = new Analysis();
        a.setMember(member);              // user_idx(FK)
        a.setAnalResult(analResultJson);
        a.setAnalRate(rate);
        a.setReportUrl(reportUrl);
        a.setCreatedAt(Timestamp.from(Instant.now()));

        Analysis saved = analysisRepository.save(a);
        return ResponseEntity.ok(new PdfCallbackResponse(saved.getAnalIdx(), "OK"));
    }

    // ===== helpers =====

    private static String text(JsonNode n, String key) {
        if (n == null || key == null) return null;
        JsonNode v = n.get(key);
        if (v == null || v.isNull()) return null;
        return v.isTextual() ? v.asText() : v.toString();
    }

    private static String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) {
            if (!isBlank(s)) return s;
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static Long longOrNull(JsonNode n, String key) {
        if (n == null) return null;
        JsonNode v = n.get(key);
        if (v == null || v.isNull()) return null;
        try {
            if (v.isNumber()) return v.longValue();
            if (v.isTextual()) return Long.parseLong(v.asText().trim());
        } catch (Exception ignore) {}
        return null;
    }

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
