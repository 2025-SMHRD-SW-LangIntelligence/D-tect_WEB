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

import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
        provider.setPasswordEncoder(encoder);                   // 내부에서 matches(raw, encoded) 사용
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager(); // DaoAuthenticationProvider 사용
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider provider) throws Exception {
        CsrfTokenRequestAttributeHandler csrfReqHandler = new CsrfTokenRequestAttributeHandler();
        csrfReqHandler.setCsrfRequestAttributeName("_csrf");

        http
                .authenticationProvider(provider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/loginPage", "/chooseRolePage",
                                "/findIdPage", "/changePasswordPage",
                                "/joinUserPage", "/userTermPage",
                                "/joinExpertPage", "/expertTermPage",
                                "/css/**", "/js/**", "/images/**",
                                "/api/members/**", "/oauth2/**"
                        ).permitAll()
                        .requestMatchers("/admin/api/experts/**").permitAll() // 인가만 해제(참고: CSRF는 아래에서 예외 처리)
                        .requestMatchers("/userMainPage", "/userMyinfoPage", "/capturePage").hasRole("USER")
                        .requestMatchers("/expertMainPage", "/expertMyinfoPage").hasRole("EXPERT")
                        .requestMatchers("/adminMainPage", "/adminUserBlockPage", "/adminInfoUpdatePage").hasRole("ADMIN")
                        .requestMatchers("/admin/api/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/loginPage")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureUrl("/loginPage?error")
                        .permitAll()
                )
                .oauth2Login(o -> o.loginPage("/loginPage").successHandler(successHandler))
                .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/").permitAll())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfReqHandler)
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/admin/api/experts/**")
                        )
                );

        return http.build();
    }
}
