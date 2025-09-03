package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.PdfcoResultItem;
import com.smhrd.dtect.entity.AnalRate;
import com.smhrd.dtect.service.pdf.PdfIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/analysis")
public class PdfcoCallbackController {

    private final PdfIngestService pdfIngestService;

    /** (A) JSON 케이스: n8n이 PDFco의 응답(JSON 배열) 그대로 보내는 경우 */
    @PostMapping(value = "/pdfco-callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> receiveJson(
        @RequestParam String sid,
        @RequestParam Long userId,
        @RequestParam AnalRate analRate,
        @RequestParam(required = false) String analResult,
        @RequestBody List<PdfcoResultItem> pdfList // ✅ 여기
    ) throws Exception {
        Long analId = pdfIngestService.saveFromJson(userId, sid, analRate, analResult, pdfList);
        return ResponseEntity.ok(new Ok(analId));
    }

    /** (B) 멀티파트 케이스: n8n HTTP Request 노드에서 Send Binary Data=true 로 파일을 보내는 경우 */
    @PostMapping(value = "/pdfco-callback", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> receiveMultipart(
            @RequestParam("sid") String sid,
            @RequestParam("userId") Long userId,
            @RequestParam("analRate") AnalRate analRate,
            @RequestParam(value = "analResult", required = false) String analResult,
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        byte[] bytes = file.getBytes();
        Long analId = pdfIngestService.saveFromBytes(userId, sid, analRate, analResult, file.getOriginalFilename(), bytes);
        return ResponseEntity.ok().body(new Ok(analId));
    }

    record Ok(Long analId) {}
}
