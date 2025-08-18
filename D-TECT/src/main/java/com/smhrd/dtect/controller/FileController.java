package com.smhrd.dtect.controller;

import com.smhrd.dtect.entity.Upload;
import com.smhrd.dtect.entity.UploadFile;
import com.smhrd.dtect.repository.UploadFileRepository;
import com.smhrd.dtect.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final UploadFileRepository uploadFileRepository;

    // 매칭 화면 (여러 파일 업로드 폼 + 목록)
    @GetMapping("/match")
    public String matchPage(@RequestParam(name = "userId",   defaultValue = "1") Long userId,
                            @RequestParam(name = "expertId", defaultValue = "1") Long expertId,
                            Model model) {

        List<Upload> uploads = fileService.list(userId, expertId);
        model.addAttribute("userId", userId);
        model.addAttribute("expertId", expertId);
        model.addAttribute("uploads", uploads);
        return "match";
    }

    // 여러 파일 업로드
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(@RequestParam Long userId,
                         @RequestParam Long expertId,
                         @RequestParam("files") List<MultipartFile> files) throws Exception {
        fileService.uploadFiles(userId, expertId, files);
        return "redirect:/match?userId=" + userId + "&expertId=" + expertId;
    }

    @GetMapping("/download/file/{fileId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long fileId) {

        // 1) 파일 메타 조회
        Optional<UploadFile> opt = uploadFileRepository.findById(fileId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UploadFile f = opt.get();

        // 2) 복호화된 바이트 얻기(실패/없음 → 404)
        byte[] bytes = fileService.downloadFilePlain(fileId);
        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

        // 3) 파일명: DB 저장 그대로 사용 (비어있으면 기본값)
        String filename = f.getFileName();
        if (filename == null || filename.isBlank()) {
            filename = "download.bin";
        }

        // 헤더 인젝션 방지(개행/따옴표만 치환)
        String safe = filename.replaceAll("[\\r\\n\"]", "_");

        String encoded = URLEncoder.encode(safe, StandardCharsets.UTF_8).replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM); //다운로드
        headers.setContentLength(bytes.length);

        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
