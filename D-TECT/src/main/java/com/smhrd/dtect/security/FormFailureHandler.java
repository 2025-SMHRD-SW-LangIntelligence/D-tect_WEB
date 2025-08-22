package com.smhrd.dtect.security;

import java.io.IOException;

import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FormFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException ex) {
        String target = "/loginPage?error"; // 기본

        if (ex instanceof LockedException || ex instanceof DisabledException) {
            target = "/loginPage?auth=blocked"; // ★ 차단 사용자 알림 루트
        } else if (ex instanceof BadCredentialsException) {
            target = "/loginPage?error=bad_credentials";
        } else if (ex instanceof CredentialsExpiredException) {
            target = "/loginPage?error=cred_expired";
        } else if (ex instanceof AccountExpiredException) {
            target = "/loginPage?error=acct_expired";
        }

        log.warn("[AUTH][FORM] failure: {} -> {}", ex.getClass().getSimpleName(), target, ex);
        try {
			getRedirectStrategy().sendRedirect(request, response, target);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}