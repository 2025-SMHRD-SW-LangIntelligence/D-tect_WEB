package com.smhrd.dtect.service;

import com.smhrd.dtect.dto.AnalysisSummaryDto;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.storage.ReportStorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    // 목록 조회
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

    // PDF 로드
    // 네이버 클라우드와 연결시 수정해야할 부분
    /*
       loadPdfBytes(Long analId) 내부를 NCP에서 getObject(key) 하도록 구현/수정.
        key는 analysis.getReportPath()에서 가져와서
        bucket + key로 다운받아 바이트 배열 반환.
        (프록시 방식 유지) 컨트롤러는 응답 헤더/파일명만 신경.
    */
    public byte[] loadPdfBytes(Long analId) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId));
        return storageClient.loadBytes(a.getReportPath());
    }

    // 결과보고서 다운로드 할때 파일 이름 설정
    // [분석 날짜] + 결과보고서
    public String buildReportFileName(Long analId) {
        var a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId));
        String date = DATE_FMT.format(a.getCreatedAt().toInstant()); // yyyy-MM-dd
        return "[" + date + "] 결과보고서.pdf";
    }

}
