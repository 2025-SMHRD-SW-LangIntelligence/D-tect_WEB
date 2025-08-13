package com.smhrd.dtect.entity;

import java.security.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "tb_upload")
public class Upload {
	
    // 자료 식별자
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long uploadIdx;

    // 회원 인덱스
	@ManyToOne
    @JoinColumn(name = "mem_idx", nullable = false)
    private Member member;

    // 자료 파일
	@Column(nullable = false)
    private String upload_file;

    // 자료 내용
	@Column(nullable = false)
    private String upload_content;

    // 자료 인코딩
	@Column(nullable = false)
    private byte[] upload_encoding;

    // 자료 벡터
	@Column(nullable = false)
    private byte[] upload_vector;

    // 업로드 날짜
	@Column(nullable = false)
    private Timestamp created_at;

}
