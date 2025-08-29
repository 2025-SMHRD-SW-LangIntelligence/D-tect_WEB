package com.smhrd.dtect.service;

import com.smhrd.dtect.dto.AnalysisSummaryDto;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final MemberRepository memberRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
    
    // ✅ username으로 목록 조회
    public List<AnalysisSummaryDto> listForUsername(String username) {
        Member m = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("member not found: " + username));
        return analysisRepository
                .findByUser_Member_MemIdxOrderByCreatedAtDesc(m.getMemIdx())
                .stream()
                .map(this::toDto)
                .toList();
    }

    private AnalysisSummaryDto toDto(Analysis a) {
        String date = DATE_FMT.format(a.getCreatedAt().toInstant());
        String fileName = "[" + date + "] 결과보고서.pdf";
        String previewUrl  = "/analysis/" + a.getAnalIdx() + "/preview";
        String downloadUrl = "/analysis/" + a.getAnalIdx() + "/download";
        return new AnalysisSummaryDto(
                a.getAnalIdx(), fileName, a.getCreatedAt(), a.getAnalRate(), previewUrl, downloadUrl
        );
    }

    public String getReportUrl(Long analId) {
        return analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId))
                .getReportUrl();
    }

    public String buildReportFileName(Long analId) {
        var a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId));
        String date = DATE_FMT.format(a.getCreatedAt().toInstant());
        return "[" + date + "] 결과보고서.pdf";
    }
}
