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

    // 매칭 화면
    @GetMapping("/match")
    public String matchPage(@RequestParam Long matchingId, Model model) {
//        List<Upload> uploads = fileService.listByMatching(matchingId);
        List<Upload> uploads = fileService.findUploadsByMatching(matchingId);
        model.addAttribute("matchingId", matchingId);
        model.addAttribute("uploads", uploads);
        return "match";
    }

    // 여러 파일 업로드
    @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadMultiple(@RequestParam Long matchingId,
                                 @RequestParam("files") List<MultipartFile> files) throws Exception {
        fileService.uploadFiles(matchingId, files);
        return "redirect:/matching/detail/" + matchingId;
    }

    // 파일 단건 다운로드
    @GetMapping("/download/file/{fileId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long fileId) {
        Optional<UploadFile> opt = uploadFileRepository.findById(fileId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        UploadFile f = opt.get();
        byte[] bytes = fileService.downloadFilePlain(fileId);
        if (bytes == null || bytes.length == 0) return ResponseEntity.notFound().build();

        String filename = StringUtils.hasText(f.getFileName()) ? f.getFileName() : "download.bin";
        String safe     = filename.replaceAll("[\\r\\n\"]", "_");
        String encoded  = URLEncoder.encode(safe, StandardCharsets.UTF_8).replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(bytes.length);

        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
