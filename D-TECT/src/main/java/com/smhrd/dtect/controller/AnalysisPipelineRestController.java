package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.*;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.service.AnalysisGrader;
import com.smhrd.dtect.service.AnalysisResultService;
import com.smhrd.dtect.service.pdf.PdfWebhookClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisPipelineRestController {

    private final AnalysisResultService analysisResultService;
    private final PdfWebhookClient pdfWebhookClient;

    @PostMapping(
        value = "/start",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AnalysisStartResponse> start(
            @RequestParam("userId") Long userId,  // Long (DB의 user_idx)
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        String sid = analysisResultService.beginSession(userId, files);
        return ResponseEntity.ok(new AnalysisStartResponse(sid));
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisStatusDto> status(@RequestParam("sid") String sid) {
        return ResponseEntity.ok(analysisResultService.getStatus(sid));
    }

    @PostMapping(value = "/finalize", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisFinalizeResponse> finalizeAnalysis(@RequestParam("sid") String sid) {
        Long userId = analysisResultService.getUserIdForSid(sid);
        if (userId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown sid");

        List<ModelMessage> items = analysisResultService.getResult(sid);
        AnalRate rate = AnalysisGrader.grade(items);
        boolean ok = pdfWebhookClient.dispatchJson(userId, sid, items, rate, null, null);

        return ResponseEntity.ok(new AnalysisFinalizeResponse(
                sid, (items != null ? items.size() : 0), rate, ok));
    }


    private static String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) if (!isBlank(s)) return s;
        return null;
    }
    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
