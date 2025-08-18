package com.smhrd.dtect.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.smhrd.dtect.service.UserDetailsServiceImpl;

//@Configuration
//public class SecurityConfiguration {
//
//    @Autowired
//    private UserDetailsServiceImpl userDetailsService;
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
//    }
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//            .authorizeHttpRequests(auth -> auth
//                .requestMatchers("/signup", "/css/**", "/js/**", "/images/**").permitAll()  // 회원가입, 정적리소스는 모두 허용
//                .anyRequest().authenticated() // 나머지 요청은 인증 필요
//            )
//            .formLogin(form -> form
//                //.loginPage("/LoginPage") // 직접 만든 로그인 페이지가 없으니 기본 로그인 페이지 사용
//                .permitAll()
//            )
//            .logout(logout -> logout
//                .permitAll()
//            );
//
//        return http.build();
//    }
//}

@Configuration
public class SecurityConfiguration {


    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 파일 업로드(POST) 테스트를 위해 CSRF 잠시 해제 (실서비스에선 유지 권장)
                .csrf(csrf -> csrf.disable())

                // 전체 허용 (가장 간단)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )

                // 로그인/로그아웃/기본 인증도 잠시 비활성화(선택)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }
}

