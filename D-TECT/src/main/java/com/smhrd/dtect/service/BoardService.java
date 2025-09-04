package com.smhrd.dtect.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smhrd.dtect.entity.Board;
import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.repository.BoardRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardService {

	private final BoardRepository boardRepository;

    // 메시지 전송
    public Board sendMessage(User user, Expert expert, String chatContent) {
        Board board = new Board();
        board.setUser(user);
        board.setExpert(expert);
        board.setChatContent(chatContent);
        return boardRepository.save(board);
    }

    // 특정 시각 이후 새로운 메시지 조회 (Polling)
    public List<Board> getNewMessages(Long userIdx, Long expertIdx, Timestamp after) {
        return boardRepository.findNewMessages(userIdx, expertIdx, after);
    }

    // 전체 대화 조회
    public List<Board> getAllMessages(Long userIdx, Long expertIdx) {
        return boardRepository.findAllMessages(userIdx, expertIdx);
    }
}
