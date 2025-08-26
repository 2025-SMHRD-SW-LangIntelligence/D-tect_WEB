package com.smhrd.dtect.config;

import com.smhrd.dtect.security.CustomOAuth2UserService;
import com.smhrd.dtect.security.FormFailureHandler;
import com.smhrd.dtect.security.OAuth2FailureHandler;
import com.smhrd.dtect.security.RoleBasedAuthenticationSuccessHandler;
import com.smhrd.dtect.security.SmartLogoutSuccessHandler;
import lombok.RequiredArgsConstructor;
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
// (선택) GET /logout 허용 시 사용
// import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final RoleBasedAuthenticationSuccessHandler successHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final FormFailureHandler formFailureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(PasswordEncoder encoder,
                                                               com.smhrd.dtect.service.UserDetailsServiceImpl userDetailsServiceimpl) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsServiceimpl);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // AJAX면 204, 브라우저면 "/"로 이동
    @Bean
    public SmartLogoutSuccessHandler smartLogoutSuccessHandler() {
        return new SmartLogoutSuccessHandler("/", 204);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider provider) throws Exception {
        http
            // 요청대로 비활성화
            .csrf(csrf -> csrf.disable())

            // 데모/개발용: 전부 허용(운영 시 필요한 경로만 permitAll로 좁히는 걸 권장)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )

            // 폼 로그인
            .formLogin(form -> form
                .loginPage("/loginPage")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(formFailureHandler)
                .permitAll()
            )

            // OAuth2 로그인
            .oauth2Login(oauth -> oauth
                .loginPage("/loginPage")
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(successHandler)
                .failureHandler(oAuth2FailureHandler)
            )

            // 🔻 로그아웃 커스터마이징
            .logout(logout -> logout
                // 기본은 POST /logout (권장)
                .logoutUrl("/logout")
                // (선택) GET /logout 허용하려면 주석 해제 (CSRF off이므로 가능하지만 보안상 비권장)
                // .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))

                .logoutSuccessHandler(smartLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "remember-me") // 사용 중인 쿠키명 추가
                .permitAll()
            )

            // 명시적으로 Provider 연결
            .authenticationProvider(provider)

            // (필요 시) 기타
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
