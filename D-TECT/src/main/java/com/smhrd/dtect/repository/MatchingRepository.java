package com.smhrd.dtect.repository;

import com.smhrd.dtect.dto.ExpertMatchingSummaryDto;
import com.smhrd.dtect.dto.UserMatchingSummaryDto;
import com.smhrd.dtect.entity.Matching;
import com.smhrd.dtect.entity.MatchingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchingRepository extends JpaRepository<Matching, Long> {

    List<Matching> findByExpert_ExpertIdxAndStatus(Long expertId, MatchingStatus status);

    // 사용자의 신청현황
    @Query("""
        select new com.smhrd.dtect.dto.UserMatchingSummaryDto(
            m.matchingIdx,
            m.requestedAt,
            em.name,
            m.requestReason,
            m.approvedAt,
            m.status
        )
        from Matching m
          join m.expert e
          join e.member em
        where m.user.userIdx = :userId
        order by m.requestedAt desc
    """)
    List<UserMatchingSummaryDto> findUserSummaries(@Param("userId") Long userId);

    // 전문가의 신청현황
    @Query("""
        select new com.smhrd.dtect.dto.ExpertMatchingSummaryDto(
            m.matchingIdx,
            m.requestedAt,
            um.name,
            m.requestReason,
            m.approvedAt,
            m.status
        )
        from Matching m
          join m.user u
          join u.member um
        where m.expert.expertIdx = :expertId
        order by m.requestedAt desc
    """)
    List<ExpertMatchingSummaryDto> findExpertSummaries(@Param("expertId") Long expertId);
}
