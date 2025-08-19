package com.smhrd.dtect.entity;


import java.io.File;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_expert")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_idx")
    private Long expertIdx;

    @Column(name = "expert_terms")
    private String expertTerms;

    @Column(name = "office_name")
    private String officeName;

    @Column(name = "office_address")
    private String officeAddress;
    
    // 변호사 인증 파일
    @Column(name = "certification_file")
    private File certificationFile;
    
    // 자료 인코딩
    @Lob
    @Column(name = "expert_encoding", columnDefinition = "MEDIUMBLOB")
    private byte[] expertEncoding;

    // 자료 벡터
    @Column(name = "expert_vector", columnDefinition = "VARBINARY(16)")
    private byte[] expertVector;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "expert_status", nullable = false)
    private ExpertStatus expertStatus;
    
    @ManyToOne
    @JoinColumn(name = "mem_idx", nullable = false)
    private Member member;
}
