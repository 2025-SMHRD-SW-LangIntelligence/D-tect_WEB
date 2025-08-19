package com.smhrd.dtect.entity;

import java.sql.Timestamp;

import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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
    @Column(name = "mem_idx")
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
	
	// 회원 전화번호
	@Column(nullable = false)
	private String phone;

    // 회원 주소
	@Column(nullable = false)
    private String address;

    // 회원 이메일
	@Column(nullable = false)
    private String email;

    // 회원 약관동의
	@Column(name = "terms_agree", nullable = false)
    private String termsAgree;

    // 회원 유형
	@Column(name = "mem_role", nullable = false)
	@Enumerated(EnumType.STRING)
    private MemRole memRole;

    // 가입 일자
	@Column(name = "joined_at", nullable = false)
    private Timestamp joinedAt;
	
	// 회원 활동 가능 여부
	@Column(name = "member_status", nullable = false)
	@Enumerated(EnumType.STRING)
	private MemberStatus memberStatus;
	
	// 날짜 자동 기입 함수
	@PrePersist
	protected void onCreate() {
	    this.joinedAt = new Timestamp(System.currentTimeMillis());
	}
	
	// 비밀번호 암호화 함수
    public void encodePassword(PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(this.password);
    }
	
}
