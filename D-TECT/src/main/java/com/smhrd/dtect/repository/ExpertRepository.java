package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.ExpertStatus;
import com.smhrd.dtect.entity.FieldName;
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

    List<Expert> findByExpertStatus(ExpertStatus status, Sort sort);
    Optional<Expert> findByMember_MemIdx(Long memIdx);
}
