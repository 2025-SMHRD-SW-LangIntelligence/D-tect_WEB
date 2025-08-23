package com.smhrd.dtect.security;

import java.util.Map;

public class GoogleUserInfo implements OAuth2UserInfo {
	
    private final Map<String, Object> attributes;

    public GoogleUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override public String getProvider() { return "google"; }
    @Override public String getProviderId() { return (String) attributes.get("sub"); }
    @Override public String getEmail() { return (String) attributes.get("email"); }
    @Override public String getName() { return (String) attributes.getOrDefault("name", ""); }
    @Override public String getImageUrl() { return (String) attributes.getOrDefault("picture", ""); }
    
}