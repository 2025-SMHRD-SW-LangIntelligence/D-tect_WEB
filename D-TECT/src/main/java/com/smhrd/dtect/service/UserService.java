package com.smhrd.dtect.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smhrd.dtect.SignupRequestDTO;
import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.ExpertStatus;
import com.smhrd.dtect.entity.MemRole;
import com.smhrd.dtect.entity.Member;
import com.smhrd.dtect.entity.MemberStatus;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.MemberRepository;
import com.smhrd.dtect.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {
	
    @Autowired
    private JavaMailSender mailSender;
    
    private final Map<String, VerificationInfo> verificationCodes = new HashMap<>();
	
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ExpertRepository expertRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileService fileService;

    public UserService(MemberRepository memberRepository, UserRepository userRepository, ExpertRepository expertRepository, PasswordEncoder passwordEncoder,
    				   FileService fileService) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.expertRepository = expertRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileService = fileService;
    }
    
    
    /**
     * 인증정보 보관용 클래스
     */
    private static class VerificationInfo {
        private final String code;
        private final long createdAt;

        public VerificationInfo(String code, long createdAt) {
            this.code = code;
            this.createdAt = createdAt;
        }
        public String getCode() { return code; }
        public long getCreatedAt() { return createdAt; }
    }
    
    /* RestController용 아이디 검증 메서드 */
    public boolean isUsernameAvailable(String username) {
        return !memberRepository.existsByUsername(username);
    }
    
    
    @Transactional
    public void registerMember(SignupRequestDTO req, MultipartFile certificationFile) throws IOException {
        if (memberRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("이미 존재하는 아이디입니다.");
        }

        Member member = new Member();
        member.setUsername(req.getUsername());
        member.setPassword(passwordEncoder.encode(req.getPassword()));
        member.setName(req.getName());
        member.setPhone(req.getPhone());
        member.setAddress(req.getAddress());
        member.setEmail(req.getEmail());
        member.setTermsAgree(req.getTermsAgree());
        member.setMemberStatus(MemberStatus.ACTIVE);
        member.setMemRole(req.isExpert() ? MemRole.EXPERT : MemRole.USER);

        memberRepository.save(member);

        if (req.isExpert()) {
            Expert expert = new Expert();
            expert.setMember(member);
            expert.setOfficeName(req.getOfficeName());
            expert.setOfficeAddress(req.getOfficeAddress());
            expert.setCertificationFile(fileService.saveFileToServer(certificationFile)); // 여기서 처리
            expert.setExpertStatus(ExpertStatus.PENDING);
            expertRepository.save(expert);
        } else {
            User user = new User();
            user.setMember(member);
            user.setUserTerms(req.getTermsAgree());
            userRepository.save(user);
        }
    }


	/**
     * 이메일 인증번호 발송
     */
    public void sendVerificationEmail(String toEmail) {
    	
    	// Gmail API 속성 강제 적용 코드
    	JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    	mailSender.setHost("smtp.gmail.com");
    	mailSender.setPort(587);
    	mailSender.setUsername("wfos3241@gmail.com");
    	mailSender.setPassword("xcleevqbbepkctqh");

    	Properties props = mailSender.getJavaMailProperties();
    	props.put("mail.transport.protocol", "smtp");
    	props.put("mail.smtp.auth", "true");
    	props.put("mail.smtp.starttls.enable", "true");
    	props.put("mail.smtp.starttls.required", "true");
    	props.put("mail.debug", "true"); // 디버그 로그 켜기
    	
        String code = generateCode();
        verificationCodes.put(toEmail, new VerificationInfo(code, System.currentTimeMillis())); // 메모리에 저장

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("회원가입 이메일 인증번호");
        msg.setText("인증번호는 [" + code + "] 입니다. \n\n5분 이내에 입력해 주세요.");
        mailSender.send(msg);
    }

    
    /**
     * 인증번호 검증 (유효시간 5분)
     */
    public boolean verifyEmailCode(String email, String code) {
        VerificationInfo info = verificationCodes.get(email);

        if (info == null) return false;

        long now = System.currentTimeMillis();
        long elapsed = now - info.getCreatedAt();

        // 5분(300초 * 1000ms) 초과하면 만료 처리
        if (elapsed > 5 * 60 * 1000) {
            verificationCodes.remove(email); // 만료된 건 삭제
            return false;
        }

        return info.getCode().equals(code);
    }

    /**
     * 랜덤 6자리 숫자 생성
     */
    private String generateCode() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }
    
}



