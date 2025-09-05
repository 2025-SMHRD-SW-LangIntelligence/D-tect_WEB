package com.smhrd.dtect.service.pdf;

import com.smhrd.dtect.controller.AnalysisSseController;
import com.smhrd.dtect.entity.*;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.repository.CaseRepository;
import com.smhrd.dtect.service.AnalysisService;
import com.smhrd.dtect.service.AnalysisResultService;
import com.smhrd.dtect.storage.NcpS3ReportStorageWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.EnumMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfReportOrchestrator {

    private final AnalysisRepository analysisRepository;
    private final CaseRepository caseRepository;
    private final AnalysisService analysisService;
    private final AnalysisResultService analysisResultService;
    private final PdfDrawReportService drawService;
    private final PdfHtmlReportService htmlService;
    private final AnalysisSseController sseController;
    private final NcpS3ReportStorageWriter storageWriter;

    @Transactional
    public boolean generateAndStoreToS3(Long analId) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId));

        EnumMap<FieldName, Integer> counts = new EnumMap<>(FieldName.class);
        for (FieldName f : FieldName.values()) {
            long c = caseRepository.countByAnalysisAndCaseType(a, f);
            if (c > 0) counts.put(f, (int) c);
        }

        // 2) 등급
        AnalRate rate = (a.getAnalRate() != null) ? a.getAnalRate() : AnalRate.NORMAL;

        // 3) 표기명/부가 정보
        String displayName = analysisService.getNameForAnalId(analId);
        String safeName = (displayName == null || displayName.isBlank()) ? "사용자"
                : displayName.replaceAll("[/\\\\:*?\"<>|#%&+]", " ").trim();
        String username = analysisResultService.getUsernameForAnalId(analId);
        String sid = analysisResultService.getSidForAnalId(analId);

        // 4) PDF 바이트 생성 (HTML 탬플릿 우선!)
        byte[] pdf;
        try {
            pdf = htmlService.render(
                    analId,
                    sid,
                    username,
                    displayName,
                    counts,
                    rate,
                    (a.getCreatedAt()  != null ? a.getCreatedAt().toInstant()  : null),
                    (a.getFinishedAt() != null ? a.getFinishedAt().toInstant() : null)
            );
        } catch (Exception htmlFail) {
            log.warn("[PDF] HTML 렌더 실패 → draw 폴백: {}", htmlFail.toString());
            pdf = drawService.render(
                    analId,
                    displayName,
                    counts,
                    rate,
                    a.getCreatedAt(),
                    a.getFinishedAt()
            );
        }

        // 5) S3 업로드
        String datePath = LocalDate.now().toString();
        String filename = safeName + "의 결과 보고서.pdf";
        String objectKey = "reports/" + datePath + "/analysis-" + analId + "-" + filename;
        String storedKey = storageWriter.upload(objectKey, pdf, "application/pdf");

        // 6) DB에 키 저장
        a.setReportUrl(storedKey);
        if (a.getFinishedAt() == null) {
            a.setFinishedAt(new Timestamp(System.currentTimeMillis()));
        }
        analysisRepository.save(a);

        try {
            sseController.notifyReady(analId, "/api/analysis/" + analId + "/report/file");
        } catch (Exception ignore) {}

        log.info("[PDF] 분석#{} 업로드 완료 key={}", analId, storedKey);
        return true;
    }
}
