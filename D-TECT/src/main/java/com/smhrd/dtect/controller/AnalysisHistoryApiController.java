package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.AnalysisSummaryDto;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.repository.UserRepository;
import com.smhrd.dtect.service.AnalysisService;
import com.smhrd.dtect.storage.NcpS3PresignService;
import com.smhrd.dtect.storage.NcpS3ReportStorageWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisHistoryApiController {

    private final AnalysisService analysisService;
    private final NcpS3PresignService presignService;
    private final AnalysisRepository analysisRepository;
    private final UserRepository userRepository;

    private final Optional<NcpS3ReportStorageWriter> storageWriter;

    @org.springframework.beans.factory.annotation.Value("${app.analysis.stream-direct:false}")
    private boolean streamDirect;

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

        String stored = analysisService.getReportUrl(analId);
        Long userId = aOpt.get().getUser().getUserIdx();
        String username = userRepository.findMemberUsernameByUserId(userId).orElse("사용자");

        String url = presignService.presignGet(stored, Duration.ofMinutes(15), true, username); // inline
        return ResponseEntity.status(302).location(URI.create(url)).build();
    }

    /** 다운로드(attachment) — presign + username 파일명 적용 */
    @GetMapping("/{analId}/download")
    public ResponseEntity<Void> download(@PathVariable Long analId) {
        var aOpt = analysisRepository.findById(analId);
        if (aOpt.isEmpty()) return ResponseEntity.notFound().build();

        String stored = analysisService.getReportUrl(analId);
        Long userId = aOpt.get().getUser().getUserIdx();
        String username = userRepository.findMemberUsernameByUserId(userId).orElse("사용자");

        String url = presignService.presignGet(stored, Duration.ofMinutes(15), false, username); // attachment
        return ResponseEntity.status(302).location(URI.create(url)).build();
    }

    /** (신규) 무제한 미리보기 — 기본은 presign 리다이렉트, 옵션 활성화 시 직접 스트리밍 */
    @GetMapping("/{analId}/report/stream")
    public ResponseEntity<?> streamReport(@PathVariable Long analId) {
        var a = analysisRepository.findById(analId)
                .orElse(null);
        if (a == null) return ResponseEntity.notFound().build();

        String stored = analysisService.getReportUrl(analId);

        // direct stream 활성 + writer 존재 시에만 스트리밍
        if (streamDirect && storageWriter.isPresent()) {
            byte[] data = storageWriter.get().loadBytes(stored);
            if (data == null) return ResponseEntity.notFound().build();

            String filename = "report.pdf";
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filename + "\"")
                    .body(new ByteArrayResource(data));
        }

        String url = presignService.presignGet(stored, Duration.ofMinutes(15), true, "사용자");
        return ResponseEntity.status(302).location(URI.create(url)).build();
    }

    @GetMapping("/{analId}/report/file")
    public ResponseEntity<?> downloadReport(@PathVariable Long analId) {
        var a = analysisRepository.findById(analId)
                .orElse(null);
        if (a == null) return ResponseEntity.notFound().build();

        String stored = analysisService.getReportUrl(analId);

        if (streamDirect && storageWriter.isPresent()) {
            byte[] data = storageWriter.get().loadBytes(stored);
            if (data == null) return ResponseEntity.notFound().build();

            String filename = analysisService.buildReportFileName(analId);
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded)
                    .body(new ByteArrayResource(data));
        }

        String url = presignService.presignGet(stored, Duration.ofMinutes(15), false, "사용자");
        return ResponseEntity.status(302).location(URI.create(url)).build();
    }
}
