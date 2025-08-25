package com.smhrd.dtect.repository;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smhrd.dtect.entity.Chat;

public interface ChatRepository extends JpaRepository<Chat, Long> {

//	// 특정 사용자(user)와 전문가(expert) 간의 지정 시간 이후 메시지 조회
//    @Query("SELECT b FROM Chat b " +
//           "WHERE b.user.userIdx = :userIdx " +
//           "AND b.expert.expertIdx = :expertIdx " +
//           "AND b.chatedAt > :after " +
//           "ORDER BY b.chatedAt ASC")
//    List<Chat> findNewMessages(Long userIdx, Long expertIdx, Timestamp after);
//
//    // 최근 대화 내역 조회 (처음 불러올 때)
//    @Query("SELECT b FROM Chat b " +
//           "WHERE b.user.userIdx = :userIdx " +
//           "AND b.expert.expertIdx = :expertIdx " +
//           "ORDER BY b.chatedAt ASC")
//    List<Chat> findAllMessages(Long userIdx, Long expertIdx);

    List<Chat> findByMatching_MatchingIdxOrderByChatedAtAsc(Long matchingIdx);
}