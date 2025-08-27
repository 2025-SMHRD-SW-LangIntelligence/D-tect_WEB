package com.smhrd.dtect.service;

import com.smhrd.dtect.dto.AnalysisSummaryDto;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.storage.ReportStorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final ReportStorageClient storageClient;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());

    /** 목록 조회 */
    @Transactional(readOnly = true)
    public List<AnalysisSummaryDto> listForUser(Long userId) {
        return analysisRepository.findByUser_UserIdxOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    private AnalysisSummaryDto toDto(Analysis a) {
        String date = DATE_FMT.format(a.getCreatedAt().toInstant());
        String fileName = "[" + date + "] 결과보고서.pdf";
        String previewUrl = "/analysis/" + a.getAnalIdx() + "/preview";
        String downloadUrl = "/analysis/" + a.getAnalIdx() + "/download";
        return new AnalysisSummaryDto(
                a.getAnalIdx(), fileName, a.getCreatedAt(), a.getAnalRate(), previewUrl, downloadUrl
        );
    }

    /**
     * PDF 로드
     * 네이버 클라우드(NCP)로 전환 시 storageClient 구현만 교체하면 됨.
     * key는 Analysis.reportPath에 저장된 경로/URL을 사용.
     */
    @Transactional(readOnly = true)
    public byte[] loadPdfBytes(Long analId) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId));
        return storageClient.loadBytes(a.getReportPath());
    }

    /** 결과보고서 다운로드 시 파일명 규칙: [yyyy-MM-dd] 결과보고서.pdf */
    @Transactional(readOnly = true)
    public String buildReportFileName(Long analId) {
        var a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId));
        String date = DATE_FMT.format(a.getCreatedAt().toInstant());
        return "[" + date + "] 결과보고서.pdf";
    }
}
