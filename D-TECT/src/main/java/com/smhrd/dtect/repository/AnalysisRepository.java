package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    List<Analysis> findByUser_UserIdxOrderByCreatedAtDesc(Long userIdx);
    Optional<Analysis> findByAnalIdxAndUser_UserIdx(Long analIdx, Long userIdx);
}
