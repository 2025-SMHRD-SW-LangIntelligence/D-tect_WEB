package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.ExpertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpertRepository extends JpaRepository<Expert, Long> {

    // 승인된 전문가 전체
    List<Expert> findAllByExpertStatus(ExpertStatus status);

    // 간단 키워드 검색(이름/사무실명/주소)
    @Query("""
        select e
        from Expert e
        where e.expertStatus = com.smhrd.dtect.entity.ExpertStatus.APPROVED
          and (:q is null or :q = '' 
               or lower(e.officeName) like lower(concat('%', :q, '%'))
               or lower(e.officeAddress) like lower(concat('%', :q, '%'))
               or lower(e.member.name) like lower(concat('%', :q, '%')))
        order by e.expertIdx asc
    """)
    List<Expert> searchApproved(@Param("q") String q);
}
