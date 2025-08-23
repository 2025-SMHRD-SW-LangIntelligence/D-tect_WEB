//com.smhrd.dtect.security.OAuth2UserInfo.java
package com.smhrd.dtect.security;

public interface OAuth2UserInfo {
	 String getProvider();      // "google" | "kakao"
	 String getProviderId();    // sub | kakao id
	 String getEmail();
	 String getName();
	 String getImageUrl();
	}


