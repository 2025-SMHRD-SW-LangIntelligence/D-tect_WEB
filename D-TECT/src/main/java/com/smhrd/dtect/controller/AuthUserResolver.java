package com.smhrd.dtect.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

public final class AuthUserResolver {
    private AuthUserResolver() {}

    /** 보안 컨텍스트에서 username 추정 */
    public static String currentUsername(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        Object p = auth.getPrincipal();

        // OAuth2
        if (p instanceof OAuth2User ou) {
            // 자주 쓰는 클레임 후보
            Object v = firstNonNull(
                ou.getAttribute("preferred_username"),
                ou.getAttribute("username"),
                ou.getAttribute("email"),
                ou.getAttribute("login"),
                ou.getAttribute("id")  // 문자열로 쓰는 경우
            );
            return v != null ? String.valueOf(v) : null;
        }

        // 폼로그인 UserDetails
        if (p instanceof UserDetails ud) {
            return ud.getUsername();
        }

        // 문자열 principal
        if (p instanceof String s) return s;

        return null;
    }

    private static Object firstNonNull(Object... arr) {
        for (Object o : arr) if (o != null) return o;
        return null;
    }
}
