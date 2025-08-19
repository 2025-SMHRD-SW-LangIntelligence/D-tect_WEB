package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.PendingExpertDetailDto;
import com.smhrd.dtect.dto.PendingExpertDto;
import com.smhrd.dtect.service.ExpertAdminService;
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
    @GetMapping("/experts/pending")
    public String pendingExperts(Model model) {
        List<PendingExpertDetailDto> list = expertAdminService.listPendingWithDetails();
        model.addAttribute("experts", list);
        return "admin/experts";
    }

    // 승인
    @PostMapping("/experts/{expertIdx}/approve")
    public String approveExpert(@PathVariable Long expertIdx) {
        expertAdminService.approve(expertIdx);
        return "redirect:/admin/experts/pending";
    }

    // 거절 -> (전문가+전문분야+멤버 삭제)
    @PostMapping("/experts/{expertIdx}/reject")
    public String rejectExpert(@PathVariable Long expertIdx) {
        expertAdminService.reject(expertIdx);
        return "redirect:/admin/experts/pending";
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
}
