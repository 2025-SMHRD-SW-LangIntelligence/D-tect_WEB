// AnalysisHistoryApiController.java
package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.AnalysisSummaryDto;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.service.AnalysisService;
import com.smhrd.dtect.storage.NcpS3ReportStorageWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisHistoryApiController {

    private final AnalysisService analysisService;
    private final AnalysisRepository analysisRepository;
    private final NcpS3ReportStorageWriter storageWriter;

    /** 히스토리 목록 */
    @GetMapping("/user-id/{userId}/history")
    public List<AnalysisSummaryDto> historyApiByUserId(@PathVariable Long userId) {
        return analysisService.listForUserId(userId);
    }

    /** 히스토리 페이지용: 무제한 미리보기 (inline) */
    @GetMapping("/{analId}/report/stream")
    public ResponseEntity<ByteArrayResource> streamReport(@PathVariable Long analId) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("analysis not found: " + analId));

        byte[] data = storageWriter.loadBytes(a.getReportUrl());
        if (data == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"report.pdf\"")
                .body(new ByteArrayResource(data));
    }

    /** 히스토리 페이지용: 무제한 다운로드 (attachment) */
    @GetMapping("/{analId}/report/file")
    public ResponseEntity<ByteArrayResource> downloadReport(@PathVariable Long analId) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("analysis not found: " + analId));

        byte[] data = storageWriter.loadBytes(a.getReportUrl());
        if (data == null) {
            return ResponseEntity.notFound().build();
        }

        String filename = analysisService.buildReportFileName(analId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + 
                        java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8))
                .body(new ByteArrayResource(data));
    }
}
