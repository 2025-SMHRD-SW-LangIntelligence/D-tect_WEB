package com.smhrd.dtect.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.smhrd.dtect.service.UserDetailsServiceImpl;

@Configuration
public class SecurityConfiguration {
	
	// Security에 해당되는 설정 클래스
	
	@Autowired
	private UserDetailsServiceImpl userDetailsService;
	
	@Bean // 비밀번호 암호화
	public PasswordEncoder passwordEncoder() {
		
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
		
	}
	
	// Spring Security 설정기능
	
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
		
		http.csrf().disable(); // CSRF 인증 토큰 비활성화
		http.authorizeHttpRequests()  // 사용자 요청을 핸들링 하겠다
			.requestMatchers("/", "/public/**", "/css/**", "js/**", "/image/**").permitAll() // public 하위폴더에 대한 접근 혀용(전체)
			.requestMatchers("/user/**", "/expert/**", "/admin/**").authenticated() // 로그인을 해야지만 board 하위 접근 허용
			.and() // 추가조건
			.formLogin() // 내가 만든 로그인 페이지를 사용하겠다
			.loginPage("/public/login") // 해당 위치의 로그인페이지를 사용하겠다
			.defaultSuccessUrl("/user/list", false)
			.failureUrl("/public/login?error=true")
			.and() // 추가조건
			.logout() // 로그아웃 기능 설정
			.logoutUrl("/public/logout") // member/logout 으로 요청시 로그아웃 기능 실행
			.logoutSuccessUrl("/public/main"); // 로그아웃 후 이동페이지 설정
			
		http.userDetailsService(userDetailsService); // Spring Security 에 로그인 기능 장착
		
		return http.build();
			
	}			

}
