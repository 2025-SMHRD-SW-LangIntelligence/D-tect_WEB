package com.smhrd.dtect.controller;

import com.smhrd.dtect.entity.Matching;
import com.smhrd.dtect.repository.MatchingRepository;
import com.smhrd.dtect.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;
    private final MatchingRepository matchingRepository;

    // 매칭 요청 (첨부파일 1개 선택)
    @PostMapping(value = "/matching/request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String request(@RequestParam Long userId,
                          @RequestParam Long expertId,
                          @RequestParam(required = false) String message,
                          @RequestParam(name = "attachment", required = false) MultipartFile attachment) throws Exception {
        matchingService.request(userId, expertId, message, attachment);
        return "redirect:/match?userId=" + userId + "&expertId=" + expertId;
    }

    // 첨부파일 다운로드
    @GetMapping("/matching/file/{matchingId}")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long matchingId) {
        Matching m = matchingRepository.findById(matchingId).orElse(null);
        if (m == null) return ResponseEntity.notFound().build();

        byte[] bytes = matchingService.downloadAttachment(matchingId);
        if (bytes == null) return ResponseEntity.notFound().build();

        String filename = (StringUtils.hasText(m.getMatchingFile())) ? m.getMatchingFile() : "attachment.bin";
        String encoded  = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}
