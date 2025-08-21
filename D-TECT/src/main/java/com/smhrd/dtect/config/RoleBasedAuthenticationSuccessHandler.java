package com.smhrd.dtect.config;

//com.smhrd.dtect.config.RoleBasedAuthenticationSuccessHandler

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class RoleBasedAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

 @Override
 public void onAuthenticationSuccess(
         HttpServletRequest request, HttpServletResponse response, Authentication authentication)
         throws IOException, ServletException {

     String targetUrl = determineTargetUrl(authentication);
     getRedirectStrategy().sendRedirect(request, response, targetUrl); // 항상 역할별 URL로 이동
     clearAuthenticationAttributes(request);
 }

 private String determineTargetUrl(Authentication authentication) {
     boolean isAdmin = false, isExpert = false, isUser = false;

     for (GrantedAuthority auth : authentication.getAuthorities()) {
         String role = auth.getAuthority(); // "ROLE_ADMIN" 등
         if ("ROLE_ADMIN".equals(role))  { isAdmin = true; break; }
         if ("ROLE_EXPERT".equals(role)) { isExpert = true; }
         if ("ROLE_USER".equals(role))   { isUser = true; }
     }
     
     if (isAdmin)  return "/adminMainPage";
     if (isExpert) return "/expertMainPage";
     if (isUser)   return "/userMainPage";
     return "/"; // 예외 기본값
 }
}
