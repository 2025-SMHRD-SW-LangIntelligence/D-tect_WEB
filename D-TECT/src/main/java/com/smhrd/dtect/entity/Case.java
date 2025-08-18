package com.smhrd.dtect.entity;

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
@Table(name = "tb_case")
public class Case {
	
    // 유형 식별자
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="code_idx")
    private Long caseIdx;

    // 분석 식별자
	@ManyToOne
	@JoinColumn(name = "anal_idx", nullable = false)
    private Analysis analysis;

    // 분류 유형
	@Column(name = "case_type", nullable = false)
	@Enumerated(EnumType.STRING)
    private FieldName caseType;
	
}
