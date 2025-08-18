package com.smhrd.dtect.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.smhrd.dtect.entity.*;
import com.smhrd.dtect.repository.*;

@Service
public class AdminService {

    private final MemberRepository memberRepository;
    private final ExpertRepository expertRepository;

    public AdminService(MemberRepository memberRepository, ExpertRepository expertRepository) {
        this.memberRepository = memberRepository;
        this.expertRepository = expertRepository;
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public void updateMemberStatus(Long memberId, MemberStatus status) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        member.setStatus(status);
        memberRepository.save(member);
    }

    public void approveExpert(Long expertId) {
        Expert expert = expertRepository.findById(expertId)
                .orElseThrow(() -> new IllegalArgumentException("Expert not found"));

        expert.setStatus(ExpertStatus.APPROVED);
        expertRepository.save(expert);

        // 전문가 승인 시 Member 역할도 EXPERT로 변경
        Member member = expert.getMember();
        member.setMemRole(MemRole.EXPERT);
        memberRepository.save(member);
    }

	public ArrayList<Member> memberListView(Model model) {
		
		return (ArrayList<Member>)memberRepository.findAll();
		
	}
}

