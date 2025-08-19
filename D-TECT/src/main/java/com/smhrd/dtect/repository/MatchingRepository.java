package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.Matching;
import com.smhrd.dtect.entity.MatchingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchingRepository extends JpaRepository<Matching, Long> {

    List<Matching> findByExpert_ExpertIdxAndStatus(Long expertId, MatchingStatus status);
}
