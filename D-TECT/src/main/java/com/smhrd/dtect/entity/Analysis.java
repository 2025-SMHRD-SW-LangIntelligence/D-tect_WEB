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
import jakarta.persistence.PrePersist;
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
    private Long analIdx;
	
	// 자료 식별자
	@ManyToOne
	@JoinColumn(name = "user_idx", nullable = false)
	private User user;

    // 분석 결과
	@Column(name = "anal_result", columnDefinition="TEXT")
    private String analResult;

    // 분석 등급	
	@Column(name = "anal_rate")
	@Enumerated(EnumType.STRING)
    private AnalRate analRate;

    // 분석 날짜
	@Column(name = "created_at")
    private Timestamp createdAt;
	
	// 분석 보고서 다운로드 경로(네이버 클라우드)
    @Column(name = "report_url", length = 1000)
    private String reportUrl;

	// 캡쳐 종료 시
	@Column(name = "finished_at", nullable = true)
	private Timestamp finishedAt;

	// 날짜 자동 기입 함수
	@PrePersist
	protected void onCreate() {
		if (this.createdAt == null) {
			this.createdAt = new Timestamp(System.currentTimeMillis());
		}
	}

}
