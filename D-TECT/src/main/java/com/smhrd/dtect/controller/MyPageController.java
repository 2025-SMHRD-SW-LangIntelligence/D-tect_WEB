package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.ExpertMatchingSummaryDto;
import com.smhrd.dtect.dto.UserMatchingSummaryDto;
import com.smhrd.dtect.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

    private final MatchingService matchingService;

    // 사용자 마이페이지 - 신청현황
    @GetMapping("/user/{userId}/matchings")
    public String userMatchings(@PathVariable Long userId, Model model) {
        List<UserMatchingSummaryDto> items = matchingService.getUserMatchings(userId);
        model.addAttribute("items", items);
        return "mypage/user-matchings";
    }

    // 전문가 마이페이지 - 신청현황
    @GetMapping("/expert/{expertId}/matchings")
    public String expertMatchings(@PathVariable Long expertId, Model model) {
        List<ExpertMatchingSummaryDto> items = matchingService.getExpertMatchings(expertId);
        model.addAttribute("items", items);
        return "mypage/expert-matchings";
    }

}
