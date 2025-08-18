package com.smhrd.dtect.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smhrd.dtect.entity.MemRole;
import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.repository.MemberRepository;

@Service
public class UserService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Member register(Member member) {
        // 비밀번호 암호화
        member.encodePassword(passwordEncoder);

        // 기본 권한 설정
        if (member.getMemRole() == null) {
            member.setMemRole(MemRole.USER);
        }

        // 저장
        return memberRepository.save(member);
    }

}
