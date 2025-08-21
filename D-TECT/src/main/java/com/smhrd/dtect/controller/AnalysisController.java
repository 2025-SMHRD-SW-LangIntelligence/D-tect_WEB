package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.AnalysisSummaryDto;
import com.smhrd.dtect.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    // 패이지 뷰
    @GetMapping("/user/{userId}/history")
    public String historyPage(@PathVariable Long userId, Model model) {
        model.addAttribute("userId", userId);
        return "user/analysis_history";
    }

    // 목록 API
    @GetMapping("/api/user/{userId}/history")
    @ResponseBody
    public List<AnalysisSummaryDto> historyApi(@PathVariable Long userId) {
        return analysisService.listForUser(userId);
    }

    // 미리보기 (네이버 클라우드에서 가져왔다고 가정)
    // 추후 수정
    @GetMapping("/{analId}/preview")
    public ResponseEntity<?> preview(@PathVariable Long analId) {
        byte[] pdf = analysisService.loadPdfBytes(analId);
        if (pdf == null || pdf.length == 0) return ResponseEntity.notFound().build();

        String filename = analysisService.buildReportFileName(analId);
        String encoded  = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_PDF);
        h.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encoded);
        h.setContentLength(pdf.length);

        return ResponseEntity.ok().headers(h).body(new ByteArrayResource(pdf));
    }

    // 다운로드 (네이버 클라우드에서 다운로드 했다고 가정)
    // 추후 수정
    @GetMapping("/{analId}/download")
    public ResponseEntity<?> download(@PathVariable Long analId) {
        byte[] pdf = analysisService.loadPdfBytes(analId);
        if (pdf == null || pdf.length == 0) return ResponseEntity.notFound().build();

        String filename = analysisService.buildReportFileName(analId);
        String encoded  = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_PDF);
        h.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename.replace("\"","'") + "\"; filename*=UTF-8''" + encoded);
        h.setContentLength(pdf.length);

        return ResponseEntity.ok().headers(h).body(new ByteArrayResource(pdf));
    }
}
