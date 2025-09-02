package com.smhrd.dtect.controller;

import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.service.AnalysisResultService;
import com.smhrd.dtect.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;

@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisCommandController {

    private final AnalysisService analysisService;
    private final AnalysisResultService analysisResultService;

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
        log.info("[Finish] start analId={}", analId);

        Analysis a = analysisService.finish(analId);

        log.info("[Finish] before finalizeByAnalId analId={}", analId);
        boolean dispatched = analysisResultService.finalizeByAnalId(analId);
        log.info("[Finish] after finalizeByAnalId analId={} dispatched={}", analId, dispatched);

        String finished = (a.getFinishedAt() == null) ? null
                : a.getFinishedAt().toInstant().atZone(ZoneId.systemDefault()).toString();
        return ResponseEntity.ok(new FinishRes(a.getAnalIdx(), finished, dispatched));
    }

    // DTO
    public record StartReq(Long userId) {}
    public record StartRes(Long analId, String startedAt) {}
    public record FinishRes(Long analId, String finishedAt, boolean dispatched) {}
}

