package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.*;
import com.smhrd.dtect.entity.MemRole;
import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.repository.UserRepository;
import com.smhrd.dtect.security.CustomUser;
import com.smhrd.dtect.service.ExpertService;
import com.smhrd.dtect.service.MatchingService;
import com.smhrd.dtect.service.PrincipalIdService;
import com.smhrd.dtect.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

    private final MatchingService matchingService;
    private final UserService userService;
    private final ExpertService expertService;
    private final PrincipalIdService principalIdService;

    @GetMapping
    public String mypageRoot(@AuthenticationPrincipal CustomUser principal) {
        if (principal == null || principal.getMember() == null) {
            return "redirect:/loginPage";
        }
        Member member = principal.getMember();
        MemRole role = member.getMemRole();

        if (role == MemRole.ADMIN) {
            return "redirect:/adminMainPage";
        } else if (role == MemRole.EXPERT) {
            Long expertId = expertService.findExpertIdByMemIdx(member.getMemIdx());
            // expertId가 없으면 메인으로 안전 탈출
            if (expertId == null) return "redirect:/expertMainPage";
            return "redirect:/mypage/expert/" + expertId;
        } else { // USER
            Long userId = userService.findUserIdByMemIdx(member.getMemIdx());
            if (userId == null) return "redirect:/userMainPage";
            return "redirect:/mypage/user/" + userId;
        }
    }

    @GetMapping("/user")
    public String myUserPage(@AuthenticationPrincipal CustomUser principal) {
        if (principal == null) return "redirect:/loginPage";
        Long memIdx = principal.getMember().getMemIdx();
        return principalIdService.findUserIdByMemIdx(memIdx)
                .map(id -> "redirect:/mypage/user/" + id)
                .orElse("redirect:/loginPage");
    }

    // 사용자 마이페이지
    @GetMapping("/user/{userId}")
    public String userMyPage(@PathVariable Long userId, Model model) {
        model.addAttribute("userId", userId);
        UserProfileDto profile = userService.getProfile(userId);
        model.addAttribute("profile", profile);
        return "user/user_mypage";
    }

    // 사용자 - 신청현황
    @GetMapping("/api/user/{userId}/matchings")
    @ResponseBody
    public List<UserMatchingSummaryDto> userMatchingsApi(@PathVariable Long userId) {
        return matchingService.getUserMatchings(userId);
    }

    @GetMapping("/expert/{expertId}")
    public String expertMyPage(@PathVariable Long expertId, Model model) {
        model.addAttribute("expertId", expertId);

        ExpertProfileDto profile = expertService.getProfile(expertId);
        List<OptionDto> specialtyOptions = expertService.getAllSpecialtyOptions();

        model.addAttribute("profile", profile);
        model.addAttribute("specialtyOptions", specialtyOptions);

        return "expert/expert_mypage";
    }

    // 전문가 - 신청현황
    @GetMapping(value = "/api/expert/{expertId}/matchings", produces = "application/json")
    @ResponseBody
    public List<ExpertMatchingSummaryDto> expertMatchingsApi(@PathVariable Long expertId) {
        return matchingService.getExpertMatchings(expertId);
    }

}
