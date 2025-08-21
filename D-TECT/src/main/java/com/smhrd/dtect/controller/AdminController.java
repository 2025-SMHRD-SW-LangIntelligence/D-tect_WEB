package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.PendingExpertDetailDto;
import com.smhrd.dtect.dto.PendingExpertDto;
import com.smhrd.dtect.service.ExpertAdminService;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.smhrd.dtect.entity.MemberStatus;
import com.smhrd.dtect.service.AdminService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final ExpertAdminService expertAdminService;     // 전문가 검수(신규)

    public AdminController(AdminService adminService, ExpertAdminService expertAdminService) {
        this.adminService = adminService;
        this.expertAdminService = expertAdminService;
    }

    @PostMapping("/block/{id}")
    public String blockUser(@PathVariable Long id) {
        adminService.updateMemberStatus(id, MemberStatus.BLOCKED);
        return "redirect:/admin/members";
    }

    @PostMapping("/unblock/{id}")
    public String unblockUser(@PathVariable Long id) {
        adminService.updateMemberStatus(id, MemberStatus.ACTIVE);
        return "redirect:/admin/members";
    }

    // 관리자의 전문가 승인 기능

    // 승인 대기중인 전문가 목록
    @GetMapping("/api/experts/pending")
    @ResponseBody
    public List<PendingExpertDetailDto> pendingExpertsJson() {
        return expertAdminService.listPendingWithDetails();
    }

    // 일괄 승인
    @PostMapping(value = "/api/experts/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> approveBulk(@RequestBody IdsRequest req) {
        int cnt = 0;
        if (req != null && req.getIds() != null) {
            for (Long id : req.getIds()) {
                try {
                    expertAdminService.approve(id);
                    cnt++; // 성공 건수 카운트(부분 성공 대응)
                } catch (Exception ignored) { }
            }
        }
        return Map.of("ok", true, "count", cnt);
    }

    // 거절 -> (전문가+전문분야+멤버 삭제)
    @PostMapping("/api/experts/{expertIdx}/reject")
    @ResponseBody
    public Map<String, Object> rejectOne(@PathVariable Long expertIdx) {
        expertAdminService.reject(expertIdx);
        return Map.of("ok", true);
    }

    // 증빙파일 다운로드
    @GetMapping("/experts/{expertIdx}/certificate")
    public ResponseEntity<byte[]> downloadCert(@PathVariable Long expertIdx) {
        byte[] bytes = expertAdminService.downloadCertificate(expertIdx);
        if (bytes == null) return ResponseEntity.notFound().build();

        String filename = expertAdminService.getCertificateFilename(expertIdx);
        if (filename == null || filename.isBlank()) filename = "certificate.bin";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @Data
    public static class IdsRequest {
        private List<Long> ids;
    }
}
