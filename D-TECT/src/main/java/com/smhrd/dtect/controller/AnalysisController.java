package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.AnalysisSummaryDto;
import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.repository.UserRepository;
import com.smhrd.dtect.service.AnalysisService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.sql.Timestamp;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class AnalysisController {

	private final AnalysisService analysisService;
	private final AnalysisRepository analysisRepository;
	private final UserRepository userRepository;
   
 // 캡처
    // 캡처 시작
    @Transactional
    public Analysis startByUserId(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));

        Analysis a = new Analysis();
        a.setUser(u);
        a.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        return analysisRepository.save(a);
    }

    // 캡처 종료
    @Transactional
    public Analysis finish(Long analId) {
        Analysis a = analysisRepository.findById(analId)
                .orElseThrow(() -> new IllegalArgumentException("분석 없음: " + analId));
        a.setFinishedAt(new Timestamp(System.currentTimeMillis()));
        return a;
    }
}
