package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.Analysis;
import com.smhrd.dtect.entity.Case;


import com.smhrd.dtect.entity.FieldName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {
	
	@Modifying
    @Transactional
    void deleteByAnalysis_User_UserIdx(Long userIdx);

    boolean existsByAnalysisAndCaseType(Analysis analysis, FieldName caseType);

    long countByAnalysisAndCaseType(Analysis analysis, FieldName caseType);

    interface TypeCount {
        FieldName getType();
        long getCnt();
    }

    @Query("""
           select c.caseType as type, count(c) as cnt
           from Case c
           where c.analysis.analIdx = :analId
           group by c.caseType
           """)
    List<TypeCount> countByAnalysisIdGroupByType(@Param("analId") Long analId);
}

