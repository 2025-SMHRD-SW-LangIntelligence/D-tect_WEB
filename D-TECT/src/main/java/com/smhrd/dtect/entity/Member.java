package com.smhrd.dtect.entity;

import java.security.Timestamp;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "tb_member")
public class Member {

    // 회원 인덱스
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memIdx;

    // 회원 아이디
	@Column(nullable = false, unique = true)
    private String username;

    // 회원 비밀번호
	@Column(nullable = false)
    private String password;

    // 회원 이름
	@Column(nullable = false)
    private String name;

    // 회원 주소
	@Column(nullable = false)
    private String addr;

    // 회원 이메일
	@Column(nullable = false)
    private String email;

    // 회원 약관동의
	@Column(nullable = false)
    private String terms_agree;

    // 회원 유형
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
    private Mem_role mem_role;

    // 사무실 이름
	@Column(nullable = false)
    private String office_name;

    // 가입 일자
	@Column(nullable = false)
    private Timestamp joined_at;
	
}
