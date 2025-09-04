package com.smhrd.dtect.controller;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.dtect.entity.Board;
import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.UserRepository;
import com.smhrd.dtect.service.BoardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class BoardRestController {

	private final BoardService boardService;
    private final UserRepository userRepository;
    private final ExpertRepository expertRepository;

    // 메시지 전송
    @PostMapping("/send")
    public ResponseEntity<Board> sendMessage(
            @RequestParam Long userIdx,
            @RequestParam Long expertIdx,
            @RequestParam String chatContent
    ) {
        User user = userRepository.findById(userIdx).orElseThrow();
        Expert expert = expertRepository.findById(expertIdx).orElseThrow();
        Board saved = boardService.sendMessage(user, expert, chatContent);
        return ResponseEntity.ok(saved);
    }

    // 새로운 메시지 가져오기 (Polling)
    @GetMapping("/messages")
    public ResponseEntity<List<Board>> getMessages(
            @RequestParam Long userIdx,
            @RequestParam Long expertIdx,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after
    ) {
        Timestamp afterTimestamp = (after == null)
                ? Timestamp.valueOf(LocalDateTime.now().minusMinutes(5))
                : Timestamp.valueOf(after);

        List<Board> newMessages = boardService.getNewMessages(userIdx, expertIdx, afterTimestamp);
        return ResponseEntity.ok(newMessages);
    }

    // 전체 대화 가져오기
    @GetMapping("/history")
    public ResponseEntity<List<Board>> getHistory(
            @RequestParam Long userIdx,
            @RequestParam Long expertIdx
    ) {
        List<Board> history = boardService.getAllMessages(userIdx, expertIdx);
        return ResponseEntity.ok(history);
    }
}

