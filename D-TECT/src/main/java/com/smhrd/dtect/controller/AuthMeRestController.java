package com.smhrd.dtect.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;

import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.security.CustomOAuth2User;


public class AuthMeRestController {

    @GetMapping("/auth/me")
    public Map<String, Object> me(@AuthenticationPrincipal CustomOAuth2User principal) {
        if (principal == null) return Map.of("authenticated", false);
        Member m = principal.getMember();
        return Map.of(
            "authenticated", true,
            "email", m.getEmail(),
            "name", m.getName(),
            "role", m.getMemRole().name(),
            "provider", m.getOauthProvider(),
            "providerId", m.getOauthId(),
            "profileImageUrl", m.getProfileImageUrl()
        );
    }
}