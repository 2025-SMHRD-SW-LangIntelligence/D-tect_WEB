package com.smhrd.dtect.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.smhrd.dtect.entity.FieldName;
import com.smhrd.dtect.service.AnalysisResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisCallbackRestController {

    private final AnalysisResultService analysisResultService;

    /**
     * 모델 서버 → 우리 서버 (유형만 누적)
     * 예: POST /api/analysis/callback?sid=ABC123&total=100
     * body: JSON 배열(각 항목에 최소 type 존재) 또는 {"results":[...]}
     */
    @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> callback(
            @RequestParam("sid") String sid,
            @RequestParam(value = "total", required = false) Long total,
            @RequestBody JsonNode body
    ) {
        JsonNode arr = body.isArray() ? body : body.path("results");
        List<FieldName> types = new ArrayList<>();

        if (arr.isArray()) {
            for (JsonNode e : arr) {
                String typeStr = null;
                if (e.hasNonNull("type")) typeStr = e.get("type").asText(null);
                else if (e.hasNonNull("caseType")) typeStr = e.get("caseType").asText(null);
                else if (e.hasNonNull("label")) typeStr = e.get("label").asText(null); // 임시 호환

                if (typeStr != null) {
                    try { types.add(FieldName.valueOf(typeStr)); }
                    catch (IllegalArgumentException ex) { log.warn("Unknown type: {}", typeStr); }
                }
            }
        }

        if (!types.isEmpty()) analysisResultService.appendTypes(sid, types, total);
        return ResponseEntity.ok().build();
    }
}
