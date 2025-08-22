package com.smhrd.dtect.security;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.smhrd.dtect.entity.Member;

import lombok.Getter;

@Getter
public class CustomOAuth2User implements OAuth2User {

    private final Member member;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomOAuth2User(Member member,
                            Map<String, Object> attributes,
                            Collection<? extends GrantedAuthority> authorities) {
        this.member = member;
        this.attributes = attributes;
        this.authorities = authorities;
    }

    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getName() {
        // Security 표현용 이름(고유키). providerId 또는 DB 키 사용
        return member.getUsername(); // Member에 getUsername()이 email 또는 별칭을 반환하도록 구현되어 있다고 가정
    }

}