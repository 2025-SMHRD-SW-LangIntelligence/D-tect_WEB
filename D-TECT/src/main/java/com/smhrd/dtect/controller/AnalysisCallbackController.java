package com.smhrd.dtect.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smhrd.dtect.dto.ModelMessage;
import com.smhrd.dtect.service.AnalysisResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisCallbackController {

    private final AnalysisResultService analysisResultService;
    private final ObjectMapper om = new ObjectMapper();

    /**
     * 모델 서버 → 우리 서버
     * - 일반 결과 배열: 누적
     * 예: POST /api/analysis/callback?sid=ABC123&total=100
     */
    @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> callback(
            HttpServletRequest req,
            @RequestParam("sid") String sid,
            @RequestParam(value = "userId", required = false) Long userId, // ✅ Long로 변경
            @RequestParam(value = "total", required = false) Long total,
            @RequestBody byte[] body
    ) throws Exception {
        String s = new String(body, StandardCharsets.UTF_8);
        log.info("[Callback] sid={}, qsUserId={}, contentType={}, bodyLen={}",
                sid, userId, req.getContentType(), body != null ? body.length : 0);

        List<ModelMessage> results = om.readValue(s, new TypeReference<>() {});
        if (results != null && !results.isEmpty()) {
            analysisResultService.appendResults(sid, results, total);
        }
        return ResponseEntity.ok().build();
    }


}
