package com.smhrd.dtect.controller;

import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.Matching;
import com.smhrd.dtect.entity.MatchingStatus;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.MatchingRepository;
import com.smhrd.dtect.service.FileService;
import com.smhrd.dtect.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    private final ExpertRepository expertRepository;
    private final FileService fileService;

    // 1) 전문가 선택 페이지
    @GetMapping("/matching/select")
    public String selectPage(@RequestParam Long userId, Model model) {
        model.addAttribute("userId", userId);
        return "user/lawyers_select";
    }

    // 2) 매칭 요청 (첨부 1개 선택 가능)
    @PostMapping(value = "/matching/request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String request(@RequestParam Long userId,
                          @RequestParam Long expertId,
                          @RequestParam(required = false) String message,
                          @RequestParam(name = "attachment", required = false) MultipartFile attachment) throws Exception {
        Matching created = matchingService.request(userId, expertId, message, attachment);
        return "redirect:/matching/detail/" + created.getMatchingIdx();
    }

    // 3) 첨부파일 다운로드(매칭 생성 시 첨부)
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

    // 4) 매칭 상세 (승인 전/후 상태 및 업로드 목록 표시)
    @GetMapping("/matching/detail/{matchingId}")
    public String detail(@PathVariable Long matchingId, Model model) {
        Matching m = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new IllegalArgumentException("매칭 없음: " + matchingId));
        model.addAttribute("matching", m);
        model.addAttribute("uploads", fileService.findUploadsByMatching(matchingId));
        return "matching/detail";
    }

    // 5) 전문가 받은 요청함
    @GetMapping("/expert/inbox")
    public String expertInbox(@RequestParam Long expertId, Model model) {
        model.addAttribute("expertId", expertId);
        model.addAttribute("pending", matchingService.expertInbox(expertId));
        return "matching/inbox";
    }

    // 6) 승인/거절
    @PostMapping("/matching/{matchingId}/approve")
    public String approve(@PathVariable Long matchingId) {
        matchingService.approve(matchingId);
        return "redirect:/matching/detail/" + matchingId;
    }

    @PostMapping("/matching/{matchingId}/reject")
    public String reject(@PathVariable Long matchingId) {
        matchingService.reject(matchingId);
        return "redirect:/matching/detail/" + matchingId;
    }
}
