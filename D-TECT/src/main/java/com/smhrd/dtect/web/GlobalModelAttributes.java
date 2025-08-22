package com.smhrd.dtect.web;

import com.smhrd.dtect.security.CustomUser;
import com.smhrd.dtect.service.PrincipalIdService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final PrincipalIdService principalIdService;

    @ModelAttribute
    public void injectIds(Model model, @AuthenticationPrincipal CustomUser principal) {
        if (principal == null) return;

        Long memIdx = principal.getMember().getMemIdx();
        model.addAttribute("memId", memIdx);

        principalIdService.findUserIdByMemIdx(memIdx)
                .ifPresent(userId -> model.addAttribute("userId", userId));
        principalIdService.findExpertIdByMemIdx(memIdx)
                .ifPresent(expertId -> model.addAttribute("expertId", expertId));
    }

    @ModelAttribute("displayName")
    public String displayName(@AuthenticationPrincipal CustomUser p) {
        if (p == null || p.getMember() == null) return "관리자님";
        String name = p.getMember().getName();
        return (name != null && !name.isBlank()) ? name + " 님" : p.getUsername() + " 님";
    }
}
