package com.smhrd.dtect.security;

import java.util.Map;

@SuppressWarnings("unchecked")
public class KakaoUserInfo implements OAuth2UserInfo {
    private final Map<String, Object> attributes;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override public String getProvider() { return "kakao"; }
    @Override public String getProviderId() { return String.valueOf(attributes.get("id")); }

    @Override
    public String getEmail() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        return account != null ? (String) account.get("email") : null;
    }

    @Override
    public String getName() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account != null) {
            Map<String, Object> profile = (Map<String, Object>) account.get("profile");
            if (profile != null) {
                Object nickname = profile.get("nickname");
                if (nickname != null) return nickname.toString();
            }
        }
        Map<String, Object> props = (Map<String, Object>) attributes.get("properties");
        if (props != null && props.get("nickname") != null) {
            return props.get("nickname").toString();
        }
        return "";
    }

    @Override
    public String getImageUrl() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account != null) {
            Map<String, Object> profile = (Map<String, Object>) account.get("profile");
            if (profile != null && profile.get("profile_image_url") != null) {
                return profile.get("profile_image_url").toString();
            }
        }
        Map<String, Object> props = (Map<String, Object>) attributes.get("properties");
        if (props != null && props.get("profile_image") != null) {
            return props.get("profile_image").toString();
        }
        return "";
    }
}