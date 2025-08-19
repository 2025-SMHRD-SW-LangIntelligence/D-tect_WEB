package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.Matching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchingRepository extends JpaRepository<Matching, Long> {

    List<Matching> findByUser_UserIdxOrderByRequestedAtDesc(Long userIdx);
    List<Matching> findByExpert_ExpertIdxOrderByRequestedAtDesc(Long expertIdx);
}
