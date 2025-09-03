package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.AnalysisSummaryDto;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.repository.UserRepository;
import com.smhrd.dtect.service.AnalysisService;
import com.smhrd.dtect.storage.NcpS3PresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisHistoryApiController {

    private final AnalysisService analysisService;
    private final NcpS3PresignService presignService;
    private final AnalysisRepository analysisRepository;
    private final UserRepository userRepository;

    /** 히스토리 목록 */
    @GetMapping("/user-id/{userId}/history")
    public List<AnalysisSummaryDto> historyApiByUserId(@PathVariable Long userId) {
        return analysisService.listForUserId(userId);
    }

    /** 미리보기(inline) — presign + username 파일명 적용 */
    @GetMapping("/{analId}/preview")
    public ResponseEntity<Void> preview(@PathVariable Long analId) {
        var aOpt = analysisRepository.findById(analId);
        if (aOpt.isEmpty()) return ResponseEntity.notFound().build();

        String stored   = analysisService.getReportUrl(analId);              // DB의 URL 또는 key
        Long userId   = aOpt.get().getUser().getUserIdx();
        String username = userRepository
                .findMemberUsernameByUserId(userId)
                .orElse("사용자");
        String url      = presignService.presignGet(stored, Duration.ofMinutes(15), true, username); // inline

        return ResponseEntity.status(302).location(URI.create(url)).build();
    }

    /** 다운로드(attachment) — presign + username 파일명 적용 */
    @GetMapping("/{analId}/download")
    public ResponseEntity<Void> download(@PathVariable Long analId) {
        var aOpt = analysisRepository.findById(analId);
        if (aOpt.isEmpty()) return ResponseEntity.notFound().build();

        String stored   = analysisService.getReportUrl(analId);
        Long userId   = aOpt.get().getUser().getUserIdx();
        String username = userRepository
                .findMemberUsernameByUserId(userId)
                .orElse("사용자");
        String url      = presignService.presignGet(stored, Duration.ofMinutes(15), false, username); // attachment

        return ResponseEntity.status(302).location(URI.create(url)).build();
    }
}
