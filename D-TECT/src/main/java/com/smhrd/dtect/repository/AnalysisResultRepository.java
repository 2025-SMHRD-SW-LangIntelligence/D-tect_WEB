package com.smhrd.dtect.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smhrd.dtect.entity.Analysis;

public interface AnalysisResultRepository extends JpaRepository<Analysis, Long> {
	
	
	
}
