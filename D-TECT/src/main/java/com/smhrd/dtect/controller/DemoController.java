package com.smhrd.dtect.controller;

import com.smhrd.dtect.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {

    // 테스트를 위한 데모 입니다.
    // 추후 삭제될 예정입니다람쥐


//    private final ExpertService expertService;         // 목록 읽기용
    private final MatchingService matchingService;     // 대기/상세 읽기용 (없으면 생략)
//    private final UploadService uploadService;         // 매칭별 업로드 목록

    @GetMapping("/index")
    public String home() { return "demo/index"; }

    @GetMapping("/experts")
    public String experts(@RequestParam(required=false) Long userId, Model model) {
        model.addAttribute("userId", userId != null ? userId : 1L);
        // 서버 렌더링으로 목록을 주고 싶으면:
        // model.addAttribute("experts", expertService.listActive());
        return "demo/experts";
    }

    @GetMapping("/expert-dashboard")
    public String expertDash(@RequestParam(required=false) Long expertId, Model model) {
        model.addAttribute("expertId", expertId != null ? expertId : 1L);
        // model.addAttribute("pending", matchingService.listPending(expertId));
        return "demo/expert-dashboard";
    }

    @GetMapping("/matching-detail")
    public String matchingDetail(@RequestParam Long matchingId, Model model) {
        model.addAttribute("matchingId", matchingId);
        // model.addAttribute("uploads", uploadService.findByMatching(matchingId));
        return "demo/matching-detail";
    }
}
