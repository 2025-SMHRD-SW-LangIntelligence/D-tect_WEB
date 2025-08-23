package com.smhrd.dtect.controller;

import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.Matching;
import com.smhrd.dtect.entity.MatchingStatus;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.MatchingRepository;
import com.smhrd.dtect.service.FileService;
import com.smhrd.dtect.service.MatchingService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

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

    @GetMapping("/matching/inquiry")
    public String inquiryPage(@RequestParam Long userId,
                              @RequestParam Long expertId,
                              @RequestParam(required = false) String error,
                              Model model) {
        Expert expert = expertRepository.findById(expertId)
                .orElseThrow(() -> new IllegalArgumentException("전문가 없음: " + expertId));

        model.addAttribute("userId", userId);
        model.addAttribute("expert", expert);
        model.addAttribute("consultTypes", CONSULT_TYPES);
        model.addAttribute("error", error); // [추가]
        return "user/inquiry_checkout";
    }

    // 2) 매칭 요청 (첨부 1개 선택 가능)
    @PostMapping(value = "/matching/request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String request(@RequestParam Long userId,
                          @RequestParam Long expertId,
                          @RequestParam(required = false) String message,
                          @RequestParam String requestReason,
                          @RequestParam(name = "attachment") MultipartFile attachment,
                          RedirectAttributes ra) throws Exception {

        if (attachment == null || attachment.isEmpty()) {
            ra.addFlashAttribute("toast", "PDF 첨부는 필수입니다.");
            return "redirect:/matching/inquiry?userId=" + userId + "&expertId=" + expertId;
        }

        String filename = Optional.ofNullable(attachment.getOriginalFilename()).orElse("");
        String contentType = Optional.ofNullable(attachment.getContentType()).orElse("");
        boolean pdfByName = filename.toLowerCase().endsWith(".pdf");
        boolean pdfByMime = MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(contentType);
        if (!(pdfByName || pdfByMime)) {
            ra.addFlashAttribute("toast", "PDF 파일만 첨부할 수 있습니다.");
            return "redirect:/matching/inquiry?userId=" + userId + "&expertId=" + expertId;
        }

        Matching created = matchingService.request(userId, expertId, message, requestReason, attachment);

        return "redirect:/mypage/user/" + userId
                + "?submitted=1&matchingId=" + created.getMatchingIdx();
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

    @GetMapping(value = "/api/users/{userId}/ongoing-experts", produces = "application/json")
    @ResponseBody
    public List<Long> ongoingExperts(@PathVariable Long userId) {
        return matchingRepository.findOngoingExpertIds(
                userId,
                List.of(MatchingStatus.PENDING, MatchingStatus.APPROVED)
        );
    }

    private static final List<ConsultType> CONSULT_TYPES = List.of(
            new ConsultType("VIOLENCE",  "폭력"),
            new ConsultType("DEFAMATION","명예훼손"),
            new ConsultType("STALKING",  "스토킹"),
            new ConsultType("SEXUAL",    "성범죄"),
            new ConsultType("LEAK",      "정보유출"),
            new ConsultType("BULLYING",  "따돌림/집단괴롭힘"),
            new ConsultType("CHANTAGE",  "협박/갈취"),
            new ConsultType("EXTORTION", "공갈/갈취")
    );

    @Data
    @AllArgsConstructor
    public static class ConsultType {
        private String code;
        private String label;
    }
}
