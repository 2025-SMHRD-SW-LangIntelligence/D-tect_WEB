package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

	// 내가(memIdx) 만든 분석들 최신순
    List<Analysis> findByUser_Member_MemIdxOrderByCreatedAtDesc(Long memIdx);

    // 단건 조회: 분석 id + 해당 user(user_idx)
    Optional<Analysis> findByAnalIdxAndUser_UserIdx(Long analIdx, Long userIdx);

    // 내가(memIdx) 만든 분석 전체 삭제
    void deleteByUser_Member_MemIdx(Long memIdx);
    
    // user_idx 로 최신순
    List<Analysis> findByUser_UserIdxOrderByCreatedAtDesc(Long userIdx);
}

