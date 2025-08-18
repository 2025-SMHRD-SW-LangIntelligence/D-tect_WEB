package com.smhrd.dtect.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.repository.MemberRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
	
	@Autowired
	MemberRepository memberRepository;
	
	public Member registerUser(Member member) {
		
		return memberRepository.save(member);
		
	}

}
