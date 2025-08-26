package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.*;
import com.smhrd.dtect.service.AnalysisResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisPipelineRestController {

    private final AnalysisResultService analysisResultService;

    // 프레임 1장 수신 → 모델 호출 → 세션 누적
    @PostMapping(value = "/frames", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FrameIngestResponse ingest(
            @RequestParam("sid") String sid,
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        long received = analysisResultService.appendFrame(sid, file);
        return new FrameIngestResponse(true, received);
    }

    // 진행 상태
    @GetMapping("/status")
    public AnalysisStatusDto status(@RequestParam("sid") String sid) {
        return analysisResultService.getStatus(sid);
    }

    // 누적 결과(모델 원본 JSON 그대로)
    @GetMapping("/result")
    public List<ModelResultDto> result(@RequestParam("sid") String sid) {
        return analysisResultService.getResult(sid);
    }

    // (선택) 세션 정리
    @DeleteMapping("/session")
    public ResponseEntity<?> clear(@RequestParam("sid") String sid) {
        analysisResultService.clear(sid);
        return ResponseEntity.noContent().build();
    }
}
