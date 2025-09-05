package com.smhrd.dtect.service;

import com.smhrd.dtect.dto.AnalysisSummaryDto;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.repository.MemberRepository;
import com.smhrd.dtect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
    
    // username으로 목록 조회
    public List<AnalysisSummaryDto> listForUsername(String username) {
        Member m = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("member not found: " + username));
        return analysisRepository
        		        .findByUser_Member_MemIdxOrderByCreatedAtDesc(m.getMemIdx())
        		        .stream()
        		        .filter(a -> a.getReportUrl() != null && !a.getReportUrl().isBlank())
        		        .map(this::toDto)
        		        .toList();
    }

    public List<AnalysisSummaryDto> listForUserId(Long userIdx) {
        return analysisRepository
            .findByUser_UserIdxOrderByCreatedAtDesc(userIdx)
            .stream()
            .filter(a -> a.getReportUrl() != null && !a.getReportUrl().isBlank())
            .map(this::toDto)
            .toList();
    }

    private AnalysisSummaryDto toDto(Analysis a) {
        String date = DATE_FMT.format(a.getCreatedAt().toInstant());
        String fileName   = "[" + date + "] 결과보고서.pdf";

        // ✅ presigned URL 대신 무제한 API 경로 사용
        String previewUrl = "/api/analysis/" + a.getAnalIdx() + "/report/stream";
        String downloadUrl= "/api/analysis/" + a.getAnalIdx() + "/report/file";

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


    // 캡처
    // 캡처 시작
    @Transactional
    public Analysis startByUserId(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));

        Analysis a = new Analysis();
        a.setUser(u);
        a.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        return analysisRepository.save(a);
    }

    // 캡처 종료
    @Transactional
    public Analysis finish(Long analId) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId));
        a.setFinishedAt(new Timestamp(System.currentTimeMillis()));
        return a;
    }

    public String getNameForAnalId(Long analId) {
        return analysisRepository.findById(analId)
                .map(Analysis::getUser)
                .map(User::getMember)
                .map(Member::getName)
                .orElse("사용자");
    }
}
