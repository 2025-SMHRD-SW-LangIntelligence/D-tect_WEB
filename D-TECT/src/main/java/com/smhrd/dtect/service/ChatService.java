package com.smhrd.dtect.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import com.smhrd.dtect.entity.*;
import com.smhrd.dtect.repository.MatchingRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.smhrd.dtect.repository.ChatRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MatchingRepository matchingRepository;
    private final ChatRepository chatRepository;

    @Transactional
    public Chat writeMessage(Long matchingId, Long meMemIdx, String content, String filePathOrToken) {
        Matching m = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new IllegalArgumentException("매칭 없음: " + matchingId));

        Long userMemIdx   = (m.getUser()!=null && m.getUser().getMember()!=null) ? m.getUser().getMember().getMemIdx() : null;
        Long expertMemIdx = (m.getExpert()!=null && m.getExpert().getMember()!=null) ? m.getExpert().getMember().getMemIdx() : null;

        ChatSenderType type;
        if (Objects.equals(meMemIdx, expertMemIdx)) type = ChatSenderType.EXPERT;
        else if (Objects.equals(meMemIdx, userMemIdx)) type = ChatSenderType.USER;
        else throw new AccessDeniedException("이 매칭의 참여자가 아닙니다.");

        Chat c = new Chat();
        c.setMatching(m);
        c.setSenderType(type);
        c.setChatContent(content);
        c.setSendedFile(filePathOrToken);
        return chatRepository.save(c);
    }

    @Transactional(readOnly = true)
    public List<Chat> listMessages(Long matchingId, Long meMemIdx) {
        Matching m = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new IllegalArgumentException("매칭 없음: " + matchingId));
        Long userMemIdx   = (m.getUser()!=null && m.getUser().getMember()!=null) ? m.getUser().getMember().getMemIdx() : null;
        Long expertMemIdx = (m.getExpert()!=null && m.getExpert().getMember()!=null) ? m.getExpert().getMember().getMemIdx() : null;
        if (!Objects.equals(meMemIdx, userMemIdx) && !Objects.equals(meMemIdx, expertMemIdx)) {
            throw new AccessDeniedException("접근 권한 없음.");
        }
        return chatRepository.findByMatching_MatchingIdxOrderByChatedAtAsc(matchingId);
    }
//
//    // 메시지 전송
//    public Chat sendMessage(User user, Expert expert, String chatContent) {
//        Chat chat = new Chat();
//        chat.setUser(user);
//        chat.setExpert(expert);
//        chat.setChatContent(chatContent);
//        return boardRepository.save(chat);
//    }
//
//    // 특정 시각 이후 새로운 메시지 조회 (Polling)
//    public List<Chat> getNewMessages(Long userIdx, Long expertIdx, Timestamp after) {
//        return boardRepository.findNewMessages(userIdx, expertIdx, after);
//    }
//
//    // 전체 대화 조회
//    public List<Chat> getAllMessages(Long userIdx, Long expertIdx) {
//        return boardRepository.findAllMessages(userIdx, expertIdx);
//    }
}
