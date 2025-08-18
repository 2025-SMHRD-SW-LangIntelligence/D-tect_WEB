package com.smhrd.dtect.entity;

import java.sql.Timestamp;



import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_analysis")
public class Analysis {
	
    // 분석 식별자
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="anal_idx")
    private Long anal_idx;

    // 자료 식별자
	@ManyToOne
	@JoinColumn(name = "user_idx", nullable = false)
    private User user;

    // 분석 결과
	@Column(name = "anal_result", columnDefinition="TEXT", nullable = false)
    private String anal_result;

    // 분석 등급
	@Column(name = "anal_rate", nullable = false)
	@Enumerated(EnumType.STRING)
    private AnalRate anal_rate;

    // 분석 날짜
	@Column(name = "created_at", nullable = false)
    private Timestamp created_at;
	
	// 결과 보고서 경로
	@Column(name = "report_path",nullable = false)
	private String report_path;
}
