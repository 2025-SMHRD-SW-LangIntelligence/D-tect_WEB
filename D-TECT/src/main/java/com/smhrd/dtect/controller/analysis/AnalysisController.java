package com.smhrd.dtect.controller.analysis;

import com.smhrd.dtect.service.analysis.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Controller
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/analysis/user/id/{memIdx}/history")
    public String historyPageById(@PathVariable Long memIdx, Model model) {
        model.addAttribute("memIdx", memIdx);
        return "user/analysis_history";
    }

    @GetMapping("/user/{username}/history")
    public String historyPage(@PathVariable String username, Model model) {
        model.addAttribute("username", username);
        return "user/analysis_history";
    }

    @GetMapping("/{analId}/preview")
    public ResponseEntity<Void> preview(@PathVariable Long analId) {
        return ResponseEntity.status(302)
                .location(URI.create("/api/analysis/" + analId + "/preview"))
                .build();
    }

    @GetMapping("/{analId}/download")
    public ResponseEntity<Void> download(@PathVariable Long analId) {
        return ResponseEntity.status(302)
                .location(URI.create("/api/analysis/" + analId + "/download"))
                .build();
    }
}
