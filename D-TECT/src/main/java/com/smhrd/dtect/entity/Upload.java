package com.smhrd.dtect.entity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
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

    // 업로드 날짜
	@Column(name = "created_at")
    private Timestamp createdAt;
	
	// 날짜 자동 기입 함수
	@PrePersist
	protected void onCreate() {
	    this.createdAt = new Timestamp(System.currentTimeMillis());
	}
	
    // 양방향 매핑
    @OneToMany(mappedBy = "upload", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UploadFile> uploadFileList = new ArrayList<>();
}
