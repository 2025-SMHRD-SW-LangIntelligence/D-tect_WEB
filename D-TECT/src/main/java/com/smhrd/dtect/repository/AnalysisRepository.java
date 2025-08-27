package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    // ✅ 목록 조회 (기존: findByUser_UserIdxOrderByCreatedAtDesc)
    List<Analysis> findByMember_MemIdxOrderByCreatedAtDesc(Long memIdx);

    // ✅ 단건 조회 (기존: findByAnalIdxAndUser_UserIdx)
    Optional<Analysis> findByAnalIdxAndMember_MemIdx(Long analIdx, Long memIdx);

    // ✅ 삭제 (기존: deleteByUser_UserIdx)
    void deleteByMember_MemIdx(Long memIdx);
}

