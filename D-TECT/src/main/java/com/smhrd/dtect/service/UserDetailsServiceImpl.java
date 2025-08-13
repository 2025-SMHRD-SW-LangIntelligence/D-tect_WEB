package com.smhrd.dtect.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.smhrd.dtect.entity.CustomUser;
import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.repository.UserRepository;


@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	
	// Spring Security 내부 인증필터 로그인 실제 기능을 해주는 Class
	
	@Autowired
	private UserRepository repository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		// Spring Security 가 자체 로그인 기능을 수행하고 나서 실행되는 메소드
		// 이 메소드는 로그인을 성공할때만 실행된다
		// 로그인 성공 시 username 에는 로그인 성공한 계정의 id 가 담겨있다
		
		Member member = (repository.findByUsername(username))
				.orElseThrow(() -> new UsernameNotFoundException("해당 계정 없음"));
		
		
		return new CustomUser(member);  // <- 여기에 리턴된 값이 Context-Holder에 저장이 된다
		
	}
	
	
	
}
