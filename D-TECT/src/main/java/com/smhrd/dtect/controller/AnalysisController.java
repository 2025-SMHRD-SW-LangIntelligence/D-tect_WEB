package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.AnalysisSummaryDto;
import com.smhrd.dtect.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class AnalysisController {

	private final AnalysisService analysisService;

    // 페이지: memIdx로 진입
	@GetMapping("/analysis/user/id/{memIdx}/history")
	public String historyPageById(@PathVariable Long memIdx, Model model) {
	    model.addAttribute("memIdx", memIdx);
	    return "user/analysis_history";
	}

//    // API: memIdx로 목록
//    @GetMapping("/api/user/id/{memIdx}/history")
//    @ResponseBody
//    public List<AnalysisSummaryDto> historyApiById(@PathVariable Long memIdx) {
//        return analysisService.listForMemberId(memIdx);
//    }

    // 페이지 진입 시 username을 넘기도록 변경 (프론트도 data-user-name 속성 사용)
    @GetMapping("/user/{username}/history")
    public String historyPage(@PathVariable String username, Model model) {
        model.addAttribute("username", username);
        return "user/analysis_history";
    }

    @GetMapping("/api/user/{username}/history")
    @ResponseBody
    public List<AnalysisSummaryDto> historyApi(@PathVariable String username) {
        return analysisService.listForUsername(username);
    }

    @GetMapping("/{analId}/preview")
    public ResponseEntity<Void> preview(@PathVariable Long analId) {
        String url = analysisService.getReportUrl(analId);
        return ResponseEntity.status(302).location(URI.create(url)).build();
    }

    @GetMapping("/{analId}/download")
    public ResponseEntity<Void> download(@PathVariable Long analId) {
        String url = analysisService.getReportUrl(analId);
        return ResponseEntity.status(302).location(URI.create(url)).build();
    }
}
