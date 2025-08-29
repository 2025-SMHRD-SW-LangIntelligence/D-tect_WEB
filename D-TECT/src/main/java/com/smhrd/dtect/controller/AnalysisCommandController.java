package com.smhrd.dtect.controller;

import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisCommandController {

    private final AnalysisService analysisService;

    // 캡처 시작
    @PostMapping("/start")
    public ResponseEntity<StartRes> start(@RequestBody StartReq req) {
        if (req == null || req.userId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        Analysis a = analysisService.startByUserId(req.userId());
        return ResponseEntity.ok(new StartRes(
                a.getAnalIdx(),
                a.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toString()
        ));
    }

    // 캡처 종료
    @PostMapping("/{analId}/finish")
    public ResponseEntity<FinishRes> finish(@PathVariable Long analId) {
        Analysis a = analysisService.finish(analId);
        String finished = (a.getFinished_at() == null) ? null
                : a.getFinished_at().toInstant().atZone(ZoneId.systemDefault()).toString();
        return ResponseEntity.ok(new FinishRes(a.getAnalIdx(), finished));
    }

    // DTO
    public record StartReq(Long userId) {}
    public record StartRes(Long analId, String startedAt) {}
    public record FinishRes(Long analId, String finishedAt) {}
}

