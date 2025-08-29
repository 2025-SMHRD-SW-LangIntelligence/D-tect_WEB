package com.smhrd.dtect.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.entity.FieldName;
import com.smhrd.dtect.service.AnalysisResultService;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/capture")
@RequiredArgsConstructor
@RestController
public class CaptureRestController {

	private final AnalysisResultService analysisResultService;
	
    @PostMapping("/start")
    public ResponseEntity<StartResp> start(@RequestParam("userId") Long userId) {
        String sid = analysisResultService.beginSession(userId, null);
        return ResponseEntity.ok(new StartResp(sid, analysisResultService.getStartedAt(sid)));
    }

    @PostMapping("/stop")
    public ResponseEntity<StopResp> stop(@RequestParam("sid") String sid) {
        analysisResultService.markEnded(sid);
        var counts = analysisResultService.getTypeCounts(sid);
        var rate   = analysisResultService.gradeByCounts(counts);
        boolean dispatched = analysisResultService.finalizeNow(sid); // n8n으로 페이로드 전송(username 포함)
        return ResponseEntity.ok(new StopResp(sid, dispatched, rate, counts));
    }

    public record StartResp(String sid, Instant startedAt) {}
    public record StopResp(String sid, boolean dispatched, AnalRate analRate,
                           Map<FieldName,Integer> typeCounts) {}
}
