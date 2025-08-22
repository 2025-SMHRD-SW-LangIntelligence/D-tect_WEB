// com.smhrd.dtect.security.OAuth2FailureHandler.java
package com.smhrd.dtect.security;

import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) {
        log.error("[AUTH] OAuth2 실패: {}", exception.getMessage(), exception);
        try {
			getRedirectStrategy().sendRedirect(request, response, "/loginPage?auth=auth_failed");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
