package com.smhrd.dtect.repository.matching;

import com.smhrd.dtect.dto.matching.ExpertMatchingSummaryDto;
import com.smhrd.dtect.dto.matching.UserMatchingSummaryDto;
import com.smhrd.dtect.entity.matching.Matching;
import com.smhrd.dtect.entity.matching.MatchingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MatchingRepository extends JpaRepository<Matching, Long> {

    List<Matching> findByExpert_ExpertIdxAndStatus(Long expertId, MatchingStatus status);

    // 사용자의 신청현황
    @Query("""
        select new com.smhrd.dtect.dto.matching.UserMatchingSummaryDto(
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
        select new com.smhrd.dtect.dto.matching.ExpertMatchingSummaryDto(
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

    boolean existsByUser_UserIdxAndExpert_ExpertIdxAndIsActiveTrue(Long userId, Long expertId);

    boolean existsByUser_UserIdxAndExpert_ExpertIdxAndStatusIn(
            Long userId, Long expertId, java.util.Collection<MatchingStatus> statuses);


    // 나와 매칭된 전문가 체크
    @Query("""
    select distinct m.expert.expertIdx
    from Matching m
    where m.user.userIdx = :userId
      and m.status in :statuses
""")
    List<Long> findOngoingExpertIds(@Param("userId") Long userId,
                                    @Param("statuses") Collection<MatchingStatus> statuses);
    
    @Query("select m.matchingIdx from Matching m where m.user.userIdx = :userId")
    List<Long> findIdsByUserId(@Param("userId") Long userId);

    @Query("select m.matchingIdx from Matching m where m.expert.expertIdx = :expertId")
    List<Long> findIdsByExpertId(@Param("expertId") Long expertId);

    void deleteAllByIdInBatch(Iterable<Long> ids);
}
