package com.smhrd.dtect.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.smhrd.dtect.dto.PdfCallbackRequest;
import com.smhrd.dtect.dto.PdfCallbackResponse;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.support.AnalRateSafe;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/analysis")
public class AnalysisPdfCallbackController {

    private final AnalysisRepository analysisRepository;
    private final EntityManager em;

    @PostMapping("/pdf-callback")
    @Transactional
    public ResponseEntity<?> pdfReady(@Validated @RequestBody PdfCallbackRequest req) {
        // (선택) 서명 검증 훅: 유효하지 않으면 401
        // if (!isValidSignature()) return ResponseEntity.status(401).body("invalid signature");

        // User 존재 검증(존재하지 않으면 404)
        User userRef;
        try {
            userRef = em.getReference(User.class, req.userId());
            em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(userRef); // 강제 touch
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        // analRate 안전 변환
        AnalRate rate = AnalRateSafe.fromNullable(req.analRate());

        // analResult는 어떤 JSON 형태든 문자열화
        JsonNode resultNode = req.analResult();
        String analResultJson = (resultNode == null) ? "null" : resultNode.toString();

        // reportPath 우선, 없으면 pdf[0].url 사용
        String reportPath = Optional.ofNullable(req.reportPath())
                .filter(s -> s != null && !s.isBlank())
                .or(() -> firstPdfUrl(req.pdf()))
                .orElse(null);

        if (reportPath == null) {
            return ResponseEntity.badRequest().body("missing reportPath (or pdf[0].url)");
        }

        // 저장
        Analysis a = new Analysis();
        a.setUser(userRef);
        a.setAnalResult(analResultJson);
        a.setAnalRate(rate);
        a.setReportPath(reportPath);
        a.setCreatedAt(Timestamp.from(Instant.now()));

        Analysis saved = analysisRepository.save(a);
        return ResponseEntity.ok(new PdfCallbackResponse(saved.getAnalIdx()));
    }

    private Optional<String> firstPdfUrl(List<com.smhrd.dtect.dto.PdfcoResultItem> pdf) {
        if (pdf == null || pdf.isEmpty()) return Optional.empty();
        String url = pdf.get(0) != null ? pdf.get(0).url() : null;
        return Optional.ofNullable(url).filter(u -> !u.isBlank());
    }
}
