package com.smhrd.dtect.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_phrase")
public class Phrase {
	
    // 문구 식별자
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "phrase_idx")
    private Long phrase_idx;

    // 문구 내용
    @Column(name = "phrase_content", columnDefinition="TEXT", nullable = false)
    private String phrase_content;
	
}
