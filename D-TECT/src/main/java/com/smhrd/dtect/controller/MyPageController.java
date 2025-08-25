package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.*;
import com.smhrd.dtect.entity.MemRole;
import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.security.CustomUser;
import com.smhrd.dtect.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

    private final MatchingService matchingService;
    private final UserService userService;
    private final ExpertService expertService;
    private final PrincipalIdService principalIdService;

    // ✅ 새로 추가된 서비스
    private final MyPageService myPageService;

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
            if (expertId == null) return "redirect:/expertMainPage";
            return "redirect:/mypage/expert/" + expertId;
        } else {
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
    public String userMyPage(@PathVariable Long userId, Model model,
                             @AuthenticationPrincipal CustomUser principal) {
        model.addAttribute("userId", userId);
        UserProfileDto profile = userService.getProfile(userId);
        model.addAttribute("profile", profile);

        Long memIdx = (principal != null && principal.getMember() != null)
                ? principal.getMember().getMemIdx() : null;
        model.addAttribute("memIdx", memIdx != null ? memIdx : 0L);
        return "user/user_mypage";
    }

    // 사용자 - 신청현황
    @GetMapping("/api/user/{userId}/matchings")
    @ResponseBody
    public List<UserMatchingSummaryDto> userMatchingsApi(@PathVariable Long userId) {
        return matchingService.getUserMatchings(userId);
    }

    @GetMapping("/expert/{expertId}")
    public String expertMyPage(@PathVariable Long expertId, Model model,
                               @AuthenticationPrincipal CustomUser principal) {
        model.addAttribute("expertId", expertId);
        ExpertProfileDto profile = expertService.getProfile(expertId);
        List<OptionDto> specialtyOptions = expertService.getAllSpecialtyOptions();
        model.addAttribute("profile", profile);
        model.addAttribute("specialtyOptions", specialtyOptions);
        return "expert/expert_mypage";
    }

    @GetMapping("/expert/mainpage")
    public String expertMainPage(@RequestParam Long expertId,
                                 @AuthenticationPrincipal CustomUser principal,
                                 Model model) {
        if (expertId == null && principal != null && principal.getMember() != null) {
            Long memIdx = principal.getMember().getMemIdx();
            Long resolved = expertService.findExpertIdByMemIdx(memIdx);
            if (resolved != null) expertId = resolved;
        }
        if (expertId == null) {
            return "redirect:/loginPage";
        }
        model.addAttribute("expertId", expertId);
        return "expert/expert_mainpage";
    }

    // 전문가 - 신청현황
    @GetMapping(value = "/api/expert/{expertId}/matchings", produces = "application/json")
    @ResponseBody
    public List<ExpertMatchingSummaryDto> expertMatchingsApi(@PathVariable Long expertId) {
        return matchingService.getExpertMatchings(expertId);
    }

    // ✅ 공통: 내 정보 조회 (팝업 프리필)
    @GetMapping("/api/me")
    @ResponseBody
    public MeProfileDto me(@AuthenticationPrincipal CustomUser principal) {
        if (principal == null || principal.getMember() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return myPageService.getMe(principal.getMember().getMemIdx());
    }

    // ✅ 공통: 내 정보 수정 (현재 비밀번호 확인 + 옵션 비번변경 + 전문가 전문분야 반영)
    @PatchMapping("/api/me")
    @ResponseBody
    @Transactional
    public MeProfileDto updateMe(@AuthenticationPrincipal CustomUser principal,
                                 @RequestBody UpdateMeRequest req) {
        if (principal == null || principal.getMember() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return myPageService.updateMe(principal.getMember().getMemIdx(), req);
    }
}
