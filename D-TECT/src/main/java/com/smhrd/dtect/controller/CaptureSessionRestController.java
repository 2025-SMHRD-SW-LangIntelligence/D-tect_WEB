package com.smhrd.dtect.controller;

import java.sql.Timestamp;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.repository.UserRepository;
import com.smhrd.dtect.service.AnalysisResultService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class CaptureSessionRestController {

    private final AnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final AnalysisResultService resultService;

    @PostMapping("/begin")
    @Transactional
    public BeginResp begin(@RequestParam Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));

        Analysis a = new Analysis();
        a.setUser(u);
        a.setAnalRate(AnalRate.NORMAL); // 초기값
        a.setAnalResult("");            // 초기값
        a.setReportUrl("");             // 초기값
        a.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        Analysis saved = analysisRepository.save(a);

        String sid = resultService.beginSession(userId, saved.getAnalIdx());
        return new BeginResp(sid, saved.getAnalIdx());
    }

    public record BeginResp(String sid, Long analId) {}
}
