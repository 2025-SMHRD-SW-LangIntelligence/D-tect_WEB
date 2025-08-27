package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.Case;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {
	
	@Modifying
    @Transactional
    void deleteByAnalysis_Member_MemIdx(Long memIdx);
	
}
