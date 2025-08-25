package com.smhrd.dtect.service;

import com.smhrd.dtect.dto.MeProfileDto;
import com.smhrd.dtect.dto.UpdateMeRequest;
import com.smhrd.dtect.entity.*;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.FieldRepository;
import com.smhrd.dtect.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MyPageService {

    private final MemberRepository memberRepository;
    private final ExpertRepository expertRepository;
    private final FieldRepository fieldRepository;
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
}
