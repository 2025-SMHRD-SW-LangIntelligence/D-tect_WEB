package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.*;
import com.smhrd.dtect.service.ExpertService;
import com.smhrd.dtect.service.MatchingService;
import com.smhrd.dtect.service.UserService;
import lombok.RequiredArgsConstructor;
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

    // 사용자 마이페이지 - 신청현황
    @GetMapping("/user/{userId}")
    public String userMatchings(@PathVariable Long userId, Model model) {
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
    public String expertMatchings(@PathVariable Long expertId, Model model) {
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
