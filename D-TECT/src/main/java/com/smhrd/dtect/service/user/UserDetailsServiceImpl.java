package com.smhrd.dtect.service.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.smhrd.dtect.entity.member.Member;
import com.smhrd.dtect.entity.member.MemberStatus;
import com.smhrd.dtect.repository.member.MemberRepository;
import com.smhrd.dtect.security.custom.CustomUser;


@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	
	// Spring Security 내부 인증필터 로그인 실제 기능을 해주는 Class
	
	@Autowired
	private MemberRepository memberRepository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
	    Member member = memberRepository.findByUsername(username)
	        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

	    if (member.getMemberStatus() == MemberStatus.BLOCKED) {
	        throw new DisabledException("This account is blocked");
	    }

	    return new CustomUser(member);
	}
}
