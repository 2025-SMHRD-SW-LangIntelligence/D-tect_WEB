package com.smhrd.dtect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.smhrd.dtect.service.UserDetailsServiceImpl;


@Configuration
public class SecurityConfiguration {
    
    private final UserDetailsServiceImpl userDetailsServiceimpl;
    private final RoleBasedAuthenticationSuccessHandler successHandler;

    public SecurityConfiguration(UserDetailsServiceImpl userDetailsServiceimpl, RoleBasedAuthenticationSuccessHandler successHandler) {
        this.userDetailsServiceimpl = userDetailsServiceimpl;
        this.successHandler = successHandler;
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
    
	@Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsServiceimpl); // DB에서 사용자/비번 로드
        provider.setPasswordEncoder(encoder);               // 내부에서 matches(raw, encoded) 사용
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager(); // DaoAuthenticationProvider 사용
    }

//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                // 파일 업로드(POST) 테스트를 위해 CSRF 잠시 해제 (실서비스에선 유지 권장)
//                .csrf(csrf -> csrf.disable())
//
//                // 전체 허용 (가장 간단)
//                .authorizeHttpRequests(auth -> auth
//                        .anyRequest().permitAll()
//                )
//
//                // 로그인/로그아웃/기본 인증도 잠시 비활성화(선택)
//                .formLogin(form -> form.disable())
//                .httpBasic(basic -> basic.disable())
//                .logout(logout -> logout.disable());
//
//        return http.build();
//    }

    
/* 최종 통합시 복구할 시큐리티 코드부분 */    
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider provider) throws Exception {
        http
            .authenticationProvider(provider)
            .authorizeHttpRequests(auth -> auth
                // ✅ 비로그인 접근 허용(공용/회원가입/찾기/약관 등)
                .requestMatchers(
                    "/", "/loginPage", "/chooseRolePage",
                    "/findIdPage", "/changePasswordPage",
                    "/joinUserPage", "/userTermPage",
                    "/joinExpertPage", "/expertTermPage",
                    "/css/**", "/js/**", "/images/**"
                ).permitAll()

                // ✅ 역할별 보호 페이지
                .requestMatchers("/userMainPage", "/userMyinfoPage", "/capturePage").hasRole("USER")
                .requestMatchers("/expertMainPage", "/expertMyinfoPage").hasRole("EXPERT")
                .requestMatchers("/adminMainPage", "/adminUserBlockPage", "/adminInfoUpdatePage").hasRole("ADMIN")

                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/loginPage")           // 너의 로그인 페이지 GET
                .loginProcessingUrl("/login")      // 로그인 처리 POST 엔드포인트(스프링이 처리)
                .usernameParameter("username")     // 폼 name과 일치해야 함
                .passwordParameter("password")     // 폼 name과 일치해야 함
                .successHandler(successHandler)    // ★ 역할별 리다이렉트
                .failureUrl("/loginPage?error")    // 실패 시
                .permitAll()
            )
            .logout(l -> l
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
            // 필요 시 .csrf(csrf -> csrf.disable()) 로 조정
        ;

        return http.build();
    }
    

}


