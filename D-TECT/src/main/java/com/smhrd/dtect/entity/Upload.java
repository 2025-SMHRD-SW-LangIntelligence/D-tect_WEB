package com.smhrd.dtect.entity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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

    // 업로드 날짜
	@Column(name = "created_at")
    private Timestamp createdAt;

    // 양방향 매핑
    @OneToMany(mappedBy = "upload", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UploadFile> uploadFileList = new ArrayList<>();
}
