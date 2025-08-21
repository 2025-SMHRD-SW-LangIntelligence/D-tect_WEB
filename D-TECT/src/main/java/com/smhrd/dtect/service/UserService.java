package com.smhrd.dtect.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.smhrd.dtect.SignupRequestDTO;
import com.smhrd.dtect.entity.*;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smhrd.dtect.repository.MemberRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {


    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ExpertRepository expertRepository;
    private final FileService fileService;

    // 메모리 인증코드 저장
    private final Map<String, VerificationInfo> verificationCodes = new HashMap<>();
    private record VerificationInfo(String code, long createdAt) {}

    public boolean isUsernameAvailable(String username) {
        return !memberRepository.existsByUsername(username);
    }

    @Transactional
    public void registerMember(SignupRequestDTO req, MultipartFile certificationFile) throws IOException {
        // 0) 기본 검증
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new IllegalArgumentException("아이디를 입력하세요.");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("비밀번호가 누락되었습니다.");
        }
        if (!"Y".equalsIgnoreCase(req.getTermsAgree())) {
            throw new IllegalArgumentException("약관에 동의해야 가입할 수 있습니다.");
        }
        if (memberRepository.existsByUsername(req.getUsername())) {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        }

        // 1) 공통 Member
        Member m = new Member();
        m.setUsername(req.getUsername());
        m.setPassword(passwordEncoder.encode(req.getPassword())); // rawPassword not null 해결
        m.setName(req.getName());
        m.setPhone(req.getPhone());
        // 주소 합치기
        String fullAddr = (req.getAddress() == null ? "" : req.getAddress())
                + (req.getAddrDetail() == null || req.getAddrDetail().isBlank() ? "" : " " + req.getAddrDetail());
        m.setAddress(fullAddr.trim());
        m.setEmail(req.getEmail());
        m.setTermsAgree(req.getTermsAgree());
        m.setMemberStatus(MemberStatus.ACTIVE);
        m.setMemRole(req.isExpert() ? MemRole.EXPERT : MemRole.USER);

        memberRepository.save(m);

        // 2) 역할별 서브 엔티티
        if (req.isExpert()) {
            // 전문가: 파일 필수
            if (certificationFile == null || certificationFile.isEmpty()) {
                throw new IllegalArgumentException("전문가 가입은 자격증명서 파일 첨부가 필수입니다.");
            }

            Expert e = new Expert();
            e.setMember(m);
            e.setExpertTerms(req.getTermsAgree());
            e.setOfficeName(req.getOfficeName());
            // 사무실 주소가 따로 온다면 우선 사용, 아니면 공통 주소
            String office = (req.getOfficeAddress() != null && !req.getOfficeAddress().isBlank())
                    ? req.getOfficeAddress()
                    : fullAddr;
            e.setOfficeAddress(office);

            try {
                // 파일을 AES로 암호화하여 DB 필드에 저장
                FileService.EncodedFile ef = fileService.encryptCertificationFile(certificationFile);
                e.setCertificationFile(ef.getOriginalName()); // 원본 파일명 기록(선택)
                e.setExpertEncoding(ef.getCipher());          // MEDIUMBLOB
                e.setExpertVector(ef.getIv());                // VARBINARY(16)
            } catch (Exception ex) {
                throw new IOException("인증서 파일 암호화에 실패했습니다.", ex);
            }

            e.setExpertStatus(ExpertStatus.PENDING);
            expertRepository.save(e);

        } else {
            User u = new User();
            u.setMember(m);
            u.setUserTerms(req.getTermsAgree());
            userRepository.save(u);
        }
    }

    // ===== 이메일 인증 =====

    public void sendVerificationEmail(String toEmail) {
        String code = generateCode();
        verificationCodes.put(toEmail, new VerificationInfo(code, System.currentTimeMillis()));

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("회원가입 이메일 인증번호");
        msg.setText("인증번호는 [" + code + "] 입니다. \n\n5분 이내에 입력해 주세요.");
        mailSender.send(msg);
    }

    public boolean verifyEmailCode(String email, String code) {
        VerificationInfo info = verificationCodes.get(email);
        if (info == null) return false;
        long elapsed = System.currentTimeMillis() - info.createdAt();
        if (elapsed > 5 * 60 * 1000) { // 5분
            verificationCodes.remove(email);
            return false;
        }
            return info.code().equals(code);
    }

    private String generateCode() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }

}
