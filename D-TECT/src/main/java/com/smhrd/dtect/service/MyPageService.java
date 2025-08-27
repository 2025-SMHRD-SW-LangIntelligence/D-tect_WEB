package com.smhrd.dtect.service;

import com.smhrd.dtect.dto.MeProfileDto;
import com.smhrd.dtect.dto.UpdateMeRequest;
import com.smhrd.dtect.dto.WithdrawRequest;
import com.smhrd.dtect.entity.*;
import com.smhrd.dtect.repository.AnalysisRepository;
import com.smhrd.dtect.repository.CaseRepository;
import com.smhrd.dtect.repository.ChatRepository;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.FieldRepository;
import com.smhrd.dtect.repository.MatchingRepository;
import com.smhrd.dtect.repository.MemberRepository;
import com.smhrd.dtect.repository.UploadFileRepository;
import com.smhrd.dtect.repository.UploadRepository;
import com.smhrd.dtect.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MyPageService {

	private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ExpertRepository expertRepository;
    private final FieldRepository fieldRepository;
    private final MatchingRepository matchingRepository;
    private final ChatRepository chatRepository;
    private final CaseRepository caseRepository;
    private final AnalysisRepository analysisRepository;
    private final UploadRepository uploadRepository;
    private final FileService fileService;
    
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public MeProfileDto getMe(Long memIdx) {
        Member m = memberRepository.findById(memIdx)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        if (m.getMemRole() == MemRole.EXPERT) {
            Expert expert = expertRepository.findByMember_MemIdx(memIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "전문가 프로필을 찾을 수 없습니다."));

            var fields = fieldRepository.findAllByExpert(expert);
            List<String> codes = fields.stream().map(f -> f.getFieldName().name()).toList();

            return MeProfileDto.builder()
                .role("EXPERT")
                .memberId(m.getMemIdx())
                .name(m.getName())
                .email(m.getEmail())
                .officeName(expert.getOfficeName())
                .officeAddress(expert.getOfficeAddress())
                .specialtyCodes(codes)
                .build();
        }

        // USER/ADMIN → USER 형태로 내려줌
        return MeProfileDto.builder()
            .role("USER")
            .memberId(m.getMemIdx())
            .name(m.getName())
            .email(m.getEmail())
            .address(m.getAddress())
            .build();
    }

    public MeProfileDto updateMe(Long memIdx, UpdateMeRequest req) {
        Member m = memberRepository.findById(memIdx)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        // 현재 비밀번호 확인
        if (req.getCurrentPassword() == null
            || !passwordEncoder.matches(req.getCurrentPassword(), m.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다.");
        }

        // 비밀번호 변경
        if (req.isChangePassword()) {
            if (req.getNewPassword() == null || req.getNewPasswordConfirm() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "새 비밀번호를 입력해 주세요.");
            }
            if (!req.getNewPassword().equals(req.getNewPasswordConfirm())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "새 비밀번호 확인이 일치하지 않습니다.");
            }
            if (req.getNewPassword().length() < 8) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "새 비밀번호는 8자 이상이어야 합니다.");
            }
            m.setPassword(passwordEncoder.encode(req.getNewPassword()));
        }

        // 공통 필드
        if (req.getName() != null)  m.setName(req.getName());
        if (req.getEmail() != null) m.setEmail(req.getEmail());

        if (m.getMemRole() == MemRole.EXPERT) {
            Expert expert = expertRepository.findByMember_MemIdx(memIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "전문가 프로필을 찾을 수 없습니다."));

            if (req.getOfficeName() != null)    expert.setOfficeName(req.getOfficeName());
            if (req.getOfficeAddress() != null) expert.setOfficeAddress(req.getOfficeAddress());

            if (req.getSpecialtyCodes() != null) {
                // 기존 전문분야 삭제 후 재삽입
                fieldRepository.deleteByExpert(expert);

                var rows = req.getSpecialtyCodes().stream()
                    .map(code -> {
                        try { return FieldName.valueOf(code); } catch (Exception e) { return null; }
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(fn -> {
                        Field f = new Field();
                        f.setExpert(expert);
                        f.setFieldName(fn);
                        return f;
                    })
                    .collect(Collectors.toList());

                if (!rows.isEmpty()) fieldRepository.saveAll(rows);
            }
        } else {
            // USER
            if (req.getAddress() != null) m.setAddress(req.getAddress());
        }

        // 변경 후 최신 상태 반환
        return getMe(memIdx);
    }
    
    @Transactional
    public void withdraw(Long memIdx, WithdrawRequest req) {
        if (req == null || req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()) {
            throw new IllegalArgumentException("현재 비밀번호를 입력해 주세요.");
        }

        Member m = memberRepository.findById(memIdx)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        // ✅ BLOCKED는 탈퇴 불가
        if (m.getMemberStatus() == MemberStatus.BLOCKED) {
            throw new IllegalStateException("차단 상태에서는 탈퇴할 수 없습니다.");
        }

        if (!passwordEncoder.matches(req.getCurrentPassword(), m.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        // 하드 삭제
        hardDeleteAllFor(m);
    }

    private void hardDeleteAllFor(Member m) {
        Long memIdx = m.getMemIdx();

        Optional<User>   userOpt   = userRepository.findByMember_MemIdx(memIdx);
        Optional<Expert> expertOpt = expertRepository.findByMember_MemIdx(memIdx);

        // 이 회원이 얽힌 모든 매칭 ID 수집
        Set<Long> matchingIds = new HashSet<>();
        userOpt.ifPresent(u -> matchingIds.addAll(matchingRepository.findIdsByUserId(u.getUserIdx())));
        expertOpt.ifPresent(e -> matchingIds.addAll(matchingRepository.findIdsByExpertId(e.getExpertIdx())));

        // 1) 업로드 먼저 삭제(Upload → UploadFile orphanRemoval)
        //    회원 기준으로 전부 정리
        fileService.purgeAllUploadsOfMember(memIdx);

        // 2) 매칭 종속 자료 정리 (채팅/케이스 등)
        if (!matchingIds.isEmpty()) {
            chatRepository.deleteByMatchingIds(matchingIds);
            
            // 3) 매칭 삭제
            matchingRepository.deleteAllByIdInBatch(matchingIds);
        }

        // 4) 사용자/전문가 소유 자료 정리
        userOpt.ifPresent(u -> {
            // 1) Case 먼저 제거
            caseRepository.deleteByAnalysis_Member_MemIdx(u.getUserIdx());

            // 2) Analysis 제거
            analysisRepository.deleteByMember_MemIdx(u.getUserIdx());

            // 3) 마지막에 User 제거
            userRepository.delete(u);
        });

        expertOpt.ifPresent(e -> {
            fieldRepository.deleteByExpert_ExpertIdx(e.getExpertIdx());
            expertRepository.delete(e);
        });

        // 5) 최종 멤버 삭제
        memberRepository.delete(m);
    }
}
