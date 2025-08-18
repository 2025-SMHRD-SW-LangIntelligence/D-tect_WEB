package com.smhrd.dtect.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
	
	@Autowired
	UserRepository userRepository;
	
	public Member registerUser(Member member) {
		
		return userRepository.save(member);
		
	}

}
