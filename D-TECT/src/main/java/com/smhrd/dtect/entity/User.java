package com.smhrd.dtect.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(of = "userIdx")
@Table(name="tb_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_idx")
    private Long userIdx;

    @Column(name = "user_terms", nullable = false)
    private String userTerms;

    @ManyToOne
    @JoinColumn(name = "mem_idx", nullable = false)
    private Member member;

    // 관리자만 보유하고 있는 포인트
    // 사용자가 청구서 결제시, 그 금액의 일정 %를 관리자에게 적립시킬 예정
    // 대충 수수료느낌?
    @Column(nullable = true)
    private Long point;

}
