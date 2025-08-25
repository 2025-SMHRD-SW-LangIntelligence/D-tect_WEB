package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.ChatDto;
import com.smhrd.dtect.entity.Chat;
import com.smhrd.dtect.entity.ChatSenderType;
import com.smhrd.dtect.entity.Upload;
import com.smhrd.dtect.entity.UploadFile;
import com.smhrd.dtect.repository.UploadFileRepository;
import com.smhrd.dtect.repository.UploadRepository;
import com.smhrd.dtect.service.ChatService;
import com.smhrd.dtect.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final FileService fileService;
    private final UploadFileRepository uploadFileRepository;
    private final UploadRepository uploadRepository;

    @GetMapping("/{matchingId}/messages")
    public List<ChatDto> list(@PathVariable Long matchingId,
                              @RequestParam Long meMemIdx) {
        return chatService.listMessages(matchingId, meMemIdx).stream()
                .map(ChatDto::from)
                .toList();
    }

    @PostMapping("/{matchingId}/messages")
    public ChatDto post(@PathVariable Long matchingId,
                        @RequestParam Long meMemIdx,
                        @RequestParam String content,
                        @RequestParam(required = false) String file) {
        Chat c = chatService.writeMessage(matchingId, meMemIdx, content, file);
        return ChatDto.from(c);
    }


    @GetMapping(value = "/{matchingId}/files", produces = "application/json")
    public List<Map<String, Object>> listFiles(@PathVariable Long matchingId) {
        List<Upload> ups = fileService.findUploadsByMatching(matchingId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Upload u : ups) {
            String role = Optional.ofNullable(u.getUploaderType())
                    .map(Enum::name).orElse("UNKNOWN"); // USER / EXPERT / UNKNOWN
            String roleLower = role.equals("EXPERT") ? "expert" : role.equals("USER") ? "user" : "unknown";

            if (u.getUploadFileList() == null) continue;
            for (UploadFile f : u.getUploadFileList()) {
                out.add(Map.of(
                        "id",   f.getFileIdx(),
                        "name", f.getFileName(),
                        "url",  "/api/chat/file/" + f.getFileIdx(),
                        "ts",   u.getCreatedAt() != null ? u.getCreatedAt().getTime() : null,
                        "by",   Map.of("role", roleLower)
                ));
            }
        }
        return out;
    }

    @PostMapping(value = "/{matchingId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/json")
    public List<Map<String, Object>> uploadFiles(
            @PathVariable Long matchingId,
            @RequestParam("file") List<MultipartFile> files,
            @RequestParam Long meMemIdx      // ← 가능하면 required 로
    ) throws Exception {

        // 1) 파일 저장
        Upload saved = fileService.uploadFiles(matchingId, files);

        // 2) 업로더 역할 기록
        ChatSenderType type = ChatSenderType.USER;
        var m = saved.getMatching();
        Long userMem   = (m.getUser()!=null && m.getUser().getMember()!=null)    ? m.getUser().getMember().getMemIdx()    : null;
        Long expertMem = (m.getExpert()!=null && m.getExpert().getMember()!=null)? m.getExpert().getMember().getMemIdx()  : null;
        if (Objects.equals(meMemIdx, expertMem)) type = ChatSenderType.EXPERT;
        else if (Objects.equals(meMemIdx, userMem)) type = ChatSenderType.USER;
        saved.setUploaderType(type);
        uploadRepository.save(saved);

        String roleLower = (type == ChatSenderType.EXPERT) ? "expert" : "user";

        // 3) 방금 저장된 파일들을 DB에서 다시 읽음
        List<UploadFile> freshFiles = uploadFileRepository.findAllByUpload_UploadIdx(saved.getUploadIdx());

        // 4) 각 파일을 "채팅 메시지"로도 남김
        List<Map<String,Object>> out = new ArrayList<>();
        for (UploadFile f : freshFiles) {
            String url = "/api/chat/file/" + f.getFileIdx();

            // 메시지 생성
            chatService.writeMessage(
                    matchingId,
                    meMemIdx,
                    f.getFileName(),
                    url
            );

            out.add(Map.of(
                    "id",   f.getFileIdx(),
                    "name", f.getFileName(),
                    "url",  url,
                    "ts",   saved.getCreatedAt()!=null ? saved.getCreatedAt().getTime() : null,
                    "by",   Map.of("role", roleLower)
            ));
        }

        return out;
    }


    // 다운로드
    @GetMapping("/file/{fileId}")
    public ResponseEntity<byte[]> download(@PathVariable Long fileId) {
        var meta = uploadFileRepository.findById(fileId).orElse(null);
        if (meta == null) return ResponseEntity.notFound().build();

        byte[] plain = fileService.downloadFilePlain(fileId);
        if (plain == null) return ResponseEntity.notFound().build();

        String filename = Optional.ofNullable(meta.getFileName()).orElse("download.bin");
        String encoded  = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(plain);
    }
}
