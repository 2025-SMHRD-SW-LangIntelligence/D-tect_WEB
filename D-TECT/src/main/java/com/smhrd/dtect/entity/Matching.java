package com.smhrd.dtect.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "tb_matching")
public class Matching {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long matchingIdx;

    @Enumerated(EnumType.STRING)
    private MatchingStatus status;

    private Timestamp requestedAt;
    private Timestamp approvedAt;
    private Timestamp rejectedAt;
    private Timestamp completedAt;

    private String requestMessage;
    private Boolean isActive; // 활성 1, 비활성 0

    private String matchingFile;

    @Lob
    @Column(name = "upload_encoding", columnDefinition = "MEDIUMBLOB")
    private byte[] matchingEncoding;

    @Column(name = "upload_vector", columnDefinition = "VARBINARY(16)")
    private byte[] matchingVector;

    // 사용자 인덱스
    @ManyToOne
    @JoinColumn(name = "user_idx", nullable = false)
    private User user;

    // 전문가 인덱스
    @ManyToOne
    @JoinColumn(name = "expert_idx", nullable = false)
    private Expert expert;

    @PrePersist
    void prePersist() {
        if (status == null) status = MatchingStatus.PENDING;
        if (requestedAt == null) requestedAt = new Timestamp(System.currentTimeMillis());
        if (isActive == null) isActive = true;
    }

    @OneToMany(mappedBy = "matching", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Upload> uploads = new ArrayList<>();

}
