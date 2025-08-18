package com.smhrd.dtect.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "tb_expert")
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
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpertStatus status;
    
    @ManyToOne
    @JoinColumn(name = "mem_idx", nullable = false)
    private Member member;
}
