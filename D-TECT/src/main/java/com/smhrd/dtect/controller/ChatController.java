package com.smhrd.dtect.controller;

import com.smhrd.dtect.dto.ChatDto;
import com.smhrd.dtect.entity.Chat;
import com.smhrd.dtect.service.ChatService;
import lombok.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

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

}
