package com.smhrd.dtect.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.smhrd.dtect.entity.MemberStatus;
import com.smhrd.dtect.service.AdminService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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

    @PostMapping("/approveExpert/{id}")
    public String approveExpert(@PathVariable Long id) {
        adminService.approveExpert(id);
        return "redirect:/admin/experts";
    }
}
