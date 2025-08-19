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
import org.springframework.util.StringUtils;
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
    // 테스트를 위한 defaultValue = "1"
    // 추후 해당 코드는 수정 예정입니다람쥐
    @GetMapping("/match")
    public String matchPage(@RequestParam(name = "matchingId", defaultValue = "1") Long matchingId,
                            Model model) {

        List<Upload> uploads = fileService.list(matchingId);
        model.addAttribute("matchingId", matchingId);
        model.addAttribute("uploads", uploads); // u.uploadFileList 사용
        return "match";
    }

    // 여러 파일 업로드 (한 매칭에 다중 파일)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(@RequestParam Long matchingId,
                         @RequestParam("files") List<MultipartFile> files) throws Exception {
        fileService.uploadFiles(matchingId, files);
        return "redirect:/match?matchingId=" + matchingId;
    }

    // 파일 다운로드
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
        if (!StringUtils.hasText(filename)) {
            filename = "download.bin";
        }
        // 헤더 인젝션 방지(개행/따옴표 치환)
        String safe = filename.replaceAll("[\\r\\n\"]", "_");
        String encoded = URLEncoder.encode(safe, StandardCharsets.UTF_8).replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(bytes.length);

        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
