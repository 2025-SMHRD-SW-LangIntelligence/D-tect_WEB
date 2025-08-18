package com.smhrd.dtect.entity;

import java.util.Collection;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

import lombok.Data;

@Getter
public class CustomUser extends User{
	
	private final Member member;

	public CustomUser(Member member) {
		
		super(member.getUsername(), member.getPassword(), 
			  AuthorityUtils.createAuthorityList("ROLE_" + member.getMem_role().toString()));
		
		this.member = member;
		
	}
	
	// 정리.
	// 로그인 후 회원의 정보는 Spring Security 의 Spring Security Context-Holder에 저장된다.
	// Context-Holder에 저장되기 위해서는 UserDetails 데이터 타입으로 저장되어야 하는데 UserDetails 타입은 Interface 이기 때문에 그 Interface 를 구현한 클래스가 User 클래스이다.
	// 내가 Member 데이터 타입으로 Context-Holder에 저장하고 싶다면 클래스 생성 후 (CustomUser) User Class 를 상속받아 그 안에 Member 클래스를 넣으면 된다.
	
	
	
}
