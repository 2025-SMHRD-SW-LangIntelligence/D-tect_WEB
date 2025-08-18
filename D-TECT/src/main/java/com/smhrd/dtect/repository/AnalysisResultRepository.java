package com.smhrd.dtect.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smhrd.dtect.entity.Analysis;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisResultRepository extends JpaRepository<Analysis, Long> {
	
	
	
}
