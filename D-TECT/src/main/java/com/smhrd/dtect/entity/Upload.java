package com.smhrd.dtect.entity;

import java.sql.Timestamp;

import jakarta.persistence.*;
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
    @Column(name = "upload_idx")
    private Long uploadIdx;

    // 사용자 인덱스
	@ManyToOne
    @JoinColumn(name = "user_idx", nullable = false)
    private User user;

    // 전문가 인덱스
    @ManyToOne
    @JoinColumn(name = "expert_idx", nullable = false)
    private Expert expert;

    // 자료 내용
    @Column(name = "upload_content", columnDefinition="TEXT", nullable = false)
    private String uploadContent;

    // 자료 인코딩
	@Column(name = "upload_encoding", columnDefinition="MEDIUMTEXT", nullable = false)
    @Lob
    private String uploadEncoding;

    // 자료 벡터
	@Column(name = "upload_vector", nullable = false)
    @Lob
    private byte[] uploadVector;

    // 업로드 날짜
	@Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

}
