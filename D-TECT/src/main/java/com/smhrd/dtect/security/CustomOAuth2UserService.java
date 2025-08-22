package com.smhrd.dtect.security;

import java.util.Optional;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smhrd.dtect.entity.MemRole;
import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.entity.MemberStatus;
import com.smhrd.dtect.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User delegateUser = new DefaultOAuth2UserService().loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google"|"kakao"
        OAuth2UserInfo info = switch (registrationId) {
            case "google" -> new GoogleUserInfo(delegateUser.getAttributes());
            case "kakao"  -> new KakaoUserInfo(delegateUser.getAttributes());
            default       -> throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        };

        Member member = upsertMember(info);

        return new CustomOAuth2User(
            member,
            delegateUser.getAttributes(),
            AuthorityUtils.createAuthorityList(member.getMemRole().getRoleName())
        );
    }

    private Member upsertMember(OAuth2UserInfo info) {
        String provider = info.getProvider();
        String providerId = info.getProviderId();
        String email = info.getEmail();
        
        // 1) provider+id 매칭
        Optional<Member> byProvider = memberRepository.findByOauthProviderAndOauthId(provider, providerId);
        if (byProvider.isPresent()) {
            return patchProfile(byProvider.get(), info);
        }

        // 2) email 매칭(로컬/다른 소셜 연결)
        if (email != null && !email.isBlank()) {
            Optional<Member> byEmail = memberRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                Member m = byEmail.get();
                m.setOauthProvider(provider);
                m.setOauthId(email);
                return patchProfile(m, info);
            }
        }

        // 3) 신규 가입
        Member m = new Member();
        m.setEmail(email);
        m.setName(info.getName());
        m.setProfileImageUrl(info.getImageUrl());
        m.setOauthProvider(provider);
        m.setOauthId(providerId);
        m.setMemRole(MemRole.USER);            // 기본 권한
        m.setMemberStatus(MemberStatus.ACTIVE); // 기본 상태
        
        log.info("[AUTH] upsert provider={}, providerId={}, email={}",
                info.getProvider(), info.getProviderId(), info.getEmail());
        
        return memberRepository.save(m);
        
        
    }

    private Member patchProfile(Member m, OAuth2UserInfo info) {
        if (m.getName() == null || m.getName().isBlank()) m.setName(info.getName());
        if (m.getProfileImageUrl() == null || m.getProfileImageUrl().isBlank()) m.setProfileImageUrl(info.getImageUrl());
        if (m.getMemberStatus() == null) m.setMemberStatus(MemberStatus.ACTIVE);
        return memberRepository.save(m);
    }
}