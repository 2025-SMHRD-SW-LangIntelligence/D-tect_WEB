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
@Table(name = "tb_field")
public class Field {
	
	// 분야 인덱스
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long field_idx;

    // 회원 인덱스
	@ManyToOne
	 @JoinColumn(name = "mem_idx", nullable = false)
    private Member member;

    // 전문 분야
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
    private Field_name field_name;
	
}
