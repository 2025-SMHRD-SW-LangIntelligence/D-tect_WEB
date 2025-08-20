package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.ExpertMatchingSummaryDto;
import com.smhrd.dtect.dto.UserMatchingSummaryDto;
import com.smhrd.dtect.service.MatchingService;
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

    // 사용자 마이페이지 - 신청현황
    @GetMapping("/user/{userId}")
    public String userMatchings(@PathVariable Long userId, Model model) {
        model.addAttribute("userId", userId);
        return "user/user_mypage";
    }

    // 사용자 - 신청현황
    @GetMapping("/api/user/{userId}/matchings")
    @ResponseBody
    public List<UserMatchingSummaryDto> userMatchingsApi(@PathVariable Long userId) {
        return matchingService.getUserMatchings(userId);
    }

    // 전문가 마이페이지
    @GetMapping("/expert/{expertId}}")
    public String expertMatchings(@PathVariable Long expertId, Model model) {
        model.addAttribute("expertId", expertId);
        return "expert/expert_mypage";
    }

    // 전문가 - 신청현황
    @GetMapping(value = "/api/expert/{expertId}/matchings", produces = "application/json")
    @ResponseBody
    public List<ExpertMatchingSummaryDto> expertMatchingsApi(@PathVariable Long expertId) {
        return matchingService.getExpertMatchings(expertId);
    }

}
