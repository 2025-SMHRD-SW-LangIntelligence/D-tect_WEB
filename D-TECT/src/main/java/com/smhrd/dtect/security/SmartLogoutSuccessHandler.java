package com.smhrd.dtect.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * AJAX(또는 JSON 요청)일 땐 204 No Content,
 * 그 외 브라우저 요청은 지정한 URL로 리다이렉트.
 */
public class SmartLogoutSuccessHandler implements LogoutSuccessHandler {

    private final String redirectUrl;
    private final int ajaxStatus;

    public SmartLogoutSuccessHandler(String redirectUrl, int ajaxStatus) {
        this.redirectUrl = redirectUrl;
        this.ajaxStatus = ajaxStatus;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException {

        boolean isAjax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                || (StringUtils.hasText(request.getHeader("Accept"))
                    && request.getHeader("Accept").contains("application/json"));

        if (isAjax) {
            response.setStatus(ajaxStatus); // e.g., 204
        } else {
            response.sendRedirect(redirectUrl);
        }
    }
}
