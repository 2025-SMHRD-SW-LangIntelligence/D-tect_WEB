package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.ExpertStatus;
import com.smhrd.dtect.entity.FieldName;
import com.smhrd.dtect.entity.Member;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpertRepository extends JpaRepository<Expert, Long> {

    List<Expert> findAllByExpertStatus(ExpertStatus status);

    @Query("""
        select distinct e
        from Expert e
        join e.member m
        left join Field f on f.expert = e
        where e.expertStatus = com.smhrd.dtect.entity.ExpertStatus.APPROVED
          and (:q is null or :q = '' or
               lower(m.name) like lower(concat('%', :q, '%'))
            or lower(m.email) like lower(concat('%', :q, '%'))
            or lower(e.officeAddress) like lower(concat('%', :q, '%')))
          and (:skill is null or f.fieldName = :skill)
        """)
    List<Expert> searchApproved(
            @Param("q") String q,
            @Param("skill") FieldName skill,
            Sort sort
    );
    
    // 승인 1건(가장 최근)
    Optional<Expert> findFirstByMemberAndExpertStatusOrderByExpertIdxDesc(Member member, ExpertStatus status);

    // 상태 무관 최근 1건
    Optional<Expert> findFirstByMemberOrderByExpertIdxDesc(Member member);

    long countByMemberAndExpertStatus(Member member, ExpertStatus status);
}
