package com.smhrd.dtect.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smhrd.dtect.dto.ModelMessage;
import com.smhrd.dtect.service.AnalysisResultService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisCallbackController {

    private final AnalysisResultService analysisResultService;
    private final ObjectMapper om = new ObjectMapper();

    @PostMapping(value = "/callback-model", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> callback(
            HttpServletRequest req,
            @RequestParam("analId") Long analId,
            @RequestBody byte[] body
    ) throws Exception {
        String s = new String(body, StandardCharsets.UTF_8);
        log.info("[Callback] analId={}, contentType={}, bodyLen={}",
                analId, req.getContentType(), body != null ? body.length : 0);

        List<ModelMessage> results = om.readValue(s, new TypeReference<List<ModelMessage>>() {});
        if (results != null && !results.isEmpty()) {
            analysisResultService.saveFromCallbackMessages(analId, results);
        }
        return ResponseEntity.ok().build();
    }
}
