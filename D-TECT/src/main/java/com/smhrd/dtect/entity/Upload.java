package com.smhrd.dtect.entity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
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


    // 업로드 날짜
	@Column(name = "created_at")
    private Timestamp createdAt;
	
	// 날짜 자동 기입 함수
	@PrePersist
	protected void onCreate() {
	    this.createdAt = new Timestamp(System.currentTimeMillis());
	}

    // 매칭 테이블과 매핑
    @ManyToOne
    @JoinColumn(name = "matching_idx", nullable = false)
    private Matching matching;

    // 파일을 업로드한 사람이 누구인지
    // 전문가 or 사용자
    @Enumerated(EnumType.STRING)
    @Column(name = "uploader_type")
    private ChatSenderType uploaderType;

	
    // 양방향 매핑
    @OneToMany(mappedBy = "upload", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UploadFile> uploadFileList = new ArrayList<>();
}
