package com.smhrd.dtect.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.smhrd.dtect.entity.ExpertStatus;
import com.smhrd.dtect.entity.MemRole;
import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.entity.MemberStatus;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.MemberRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

// com.smhrd.dtect.security.RoleBasedAuthenticationSuccessHandler.java
@Component
@RequiredArgsConstructor
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	private static final Logger log = LoggerFactory.getLogger(RoleBasedAuthenticationSuccessHandler.class);
	private final ExpertRepository expertRepository;
	private final MemberRepository memberRepository; // 폼로그인 principal 처리용(이메일로 Member 조회)

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		String ctx = request.getContextPath();
		Member member = resolveMember(authentication);
		if (member == null) {
			log.warn("[AUTH] principal->Member 변환 실패 -> loginPage?error=true");
			denyLoginAndRedirect(request, response, ctx + "/loginPage?error=true");
			return;
		}

		log.info("[AUTH] login success: memIdx={}, email={}, role={}, status={}", member.getMemIdx(), member.getEmail(),
				member.getMemRole(), member.getMemberStatus());

		if (member.getMemberStatus() == MemberStatus.BLOCKED) {
			log.info("[AUTH] BLOCKED -> loginPage?auth=blocked");
			denyLoginAndRedirect(request, response, ctx + "/loginPage?auth=blocked");
			return;
		}

		if (member.getMemRole() == MemRole.ADMIN) {
			log.info("[AUTH] ADMIN -> /adminMainPage");
			response.sendRedirect(ctx + "/adminMainPage");
			return;
		}

		if (member.getMemRole() == MemRole.EXPERT) {
			
		    boolean hasApproved = expertRepository
		        .findFirstByMemberAndExpertStatusOrderByExpertIdxDesc(member, ExpertStatus.APPROVED)
		        .isPresent();

		    boolean hasPending = !hasApproved && expertRepository
		        .findFirstByMemberAndExpertStatusOrderByExpertIdxDesc(member, ExpertStatus.PENDING)
		        .isPresent();
		    
			if (hasApproved) {
				log.info("[AUTH] EXPERT(approved) -> /expertMainPage");
				response.sendRedirect(ctx + "/expertMainPage");
				return;
			}

			if (hasPending) {
				log.info("[AUTH] EXPERT(pending) -> loginPage?auth=expert_pending");
				denyLoginAndRedirect(request, response, ctx + "/loginPage?auth=expert_pending");
				return;
			}

			log.info("[AUTH] EXPERT(not registered) -> loginPage?auth=expert_register");
			denyLoginAndRedirect(request, response, ctx + "/loginPage?auth=expert_register");
			return;
		}

		log.info("[AUTH] USER -> /userMainPage");
		response.sendRedirect(ctx + "/userMainPage");
	}

	/** Authentication의 principal에서 우리 도메인 Member를 복원 */
    private Member resolveMember(Authentication authentication) {
        Object p = authentication.getPrincipal();

        // 1) OAuth2 로그인: 우리가 만든 CustomOAuth2User → 바로 Member 보유
        if (p instanceof CustomOAuth2User o) {
            return o.getMember();
        }

        // 2) 폼 로그인: 우리가 만든 CustomUser(org.springframework.security.core.userdetails.User 상속)
        if (p instanceof CustomUser cu) {
            return cu.getMember();
        }

        // 3) 일반 UserDetails만 있는 경우(아이디=이메일 가정) → DB에서 조회
        if (p instanceof UserDetails ud) {
            return memberRepository.findByEmail(ud.getUsername()).orElse(null);
        }

        // 4) 어떤 경우도 아니면 null
        return null;
    }

    /** 로그인 무효화 + 리다이렉트 */
    private void denyLoginAndRedirect(HttpServletRequest request,
                                      HttpServletResponse response,
                                      String target) throws IOException {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        response.sendRedirect(target);
    }
}
