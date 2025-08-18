package com.smhrd.dtect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.smhrd.dtect.service.UserDetailsServiceImpl;


@Configuration
public class SecurityConfiguration {
    
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfiguration(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
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

    
/* 최종 통합시 복구할 시큐리티 코드부분 */    
    
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//            .authorizeHttpRequests(auth -> auth
//                .requestMatchers("/signup", "/css/**", "/js/**", "/images/**").permitAll()
//                .requestMatchers("/admin/**").hasRole("ADMIN") // 관리자만 접근
//                .anyRequest().authenticated()
//            )
//            .formLogin(form -> form
//                .loginPage("/login")
//                .loginProcessingUrl("/login")
//                .defaultSuccessUrl("/user", true)
//                .failureUrl("/login?error")
//                .permitAll()
//            )
//            .logout(logout -> logout
//                .logoutUrl("/logout")
//                .logoutSuccessUrl("/login?logout")
//                .permitAll()
//            )
//            .userDetailsService(userDetailsService);
//
//        return http.build();
//    }
    

}


