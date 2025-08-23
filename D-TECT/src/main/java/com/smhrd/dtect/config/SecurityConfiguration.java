package com.smhrd.dtect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.smhrd.dtect.security.CustomOAuth2UserService;
import com.smhrd.dtect.security.FormFailureHandler;
import com.smhrd.dtect.security.OAuth2FailureHandler;
import com.smhrd.dtect.security.RoleBasedAuthenticationSuccessHandler;
import com.smhrd.dtect.service.UserDetailsServiceImpl;

import lombok.RequiredArgsConstructor;


@Configuration(proxyBeanMethods = false) // ★ 추가
@RequiredArgsConstructor                 // ★ 생성자 1개(필드 기반)만 유지
public class SecurityConfiguration {
    
	private final OAuth2FailureHandler oAuth2FailureHandler;
    private final RoleBasedAuthenticationSuccessHandler successHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
	private final FormFailureHandler formFailureHandler;
    private final UserDetailsServiceImpl userDetailsServiceimpl;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsServiceimpl); // DB에서 사용자/비번 로드
        provider.setPasswordEncoder(encoder);                   // 내부에서 matches(raw, encoded) 사용
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager(); // DaoAuthenticationProvider 사용
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider provider) throws Exception {
      http
      	.csrf(csrf -> csrf.disable())
      	.authorizeHttpRequests(auth -> auth
              .anyRequest().permitAll())
        .formLogin(form -> form
          .loginPage("/loginPage")
          .loginProcessingUrl("/login")
          .usernameParameter("username")
          .passwordParameter("password")
          .successHandler(successHandler)
          .failureHandler(formFailureHandler)
          .permitAll()
        )
        .oauth2Login(oauth -> oauth
                .loginPage("/loginPage") // 커스텀 로그인 페이지 사용 시
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(successHandler)
                .failureHandler(oAuth2FailureHandler)
            )
            .logout(Customizer.withDefaults());
        
        
        
        
        
        
//        .oauth2Login(o -> o.loginPage("/loginPage").successHandler(successHandler))
//        .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/").permitAll());
      // CSRF: 메타/헤더 사용(A안) 또는 api 예외(B안) 중 프로젝트 정책에 맞춰 선택
      return http.build();
    }
}
