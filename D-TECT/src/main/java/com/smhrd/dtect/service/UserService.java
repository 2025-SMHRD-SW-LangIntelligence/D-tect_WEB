// src/main/java/com/smhrd/dtect/service/UserService.java
package com.smhrd.dtect.service;


import com.smhrd.dtect.SignupRequestDTO;
import com.smhrd.dtect.entity.*;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.MemberRepository;
import com.smhrd.dtect.repository.UserRepository;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

  private final Map<String, VerificationInfo> verificationCodes = new java.util.concurrent.ConcurrentHashMap<>();

  private final JavaMailSender mailSender;
  private final PasswordEncoder passwordEncoder;
  private final MemberRepository memberRepository;
  private final UserRepository userRepository;
  private final ExpertRepository expertRepository;
  private final FileService fileService;

  private record VerificationInfo(String code, long createdAtMs) {}

  public boolean isUsernameAvailable(String username) {
    return !memberRepository.existsByUsername(username);
  }

  @Transactional
  public void registerMember(SignupRequestDTO req, MultipartFile certificationFile) throws IOException {
    // 기본 검증
    if (req.getUsername()==null || req.getUsername().isBlank()) throw new IllegalArgumentException("아이디를 입력하세요.");
    if (req.getPassword()==null || req.getPassword().isBlank()) throw new IllegalArgumentException("비밀번호가 누락되었습니다.");
    if (!"Y".equalsIgnoreCase(req.getTermsAgree())) throw new IllegalArgumentException("약관에 동의해야 가입할 수 있습니다.");
    if (memberRepository.existsByUsername(req.getUsername())) throw new IllegalStateException("이미 존재하는 아이디입니다.");

    // Member
    Member m = new Member();
    m.setUsername(req.getUsername());
    m.setPassword(passwordEncoder.encode(req.getPassword()));
    m.setName(req.getName());
    m.setPhone(req.getPhone());
    String fullAddr = (req.getAddress()==null? "": req.getAddress())
        + (req.getAddrDetail()==null || req.getAddrDetail().isBlank()? "" : " ("+req.getAddrDetail()+")");
    m.setAddress(fullAddr.trim());
    m.setEmail(req.getEmail());
    m.setTermsAgree(req.getTermsAgree());
    m.setMemberStatus(MemberStatus.ACTIVE);
    m.setMemRole(req.isExpert()? MemRole.EXPERT : MemRole.USER);
    memberRepository.save(m);

    if (req.isExpert()) {
      if (certificationFile == null || certificationFile.isEmpty())
        throw new IllegalArgumentException("전문가 가입은 자격증명서 파일 첨부가 필수입니다.");

      Expert e = new Expert();
      e.setMember(m);
      e.setExpertTerms(req.getTermsAgree());
      e.setOfficeName(req.getOfficeName());
      String office = (req.getOfficeAddress()!=null && !req.getOfficeAddress().isBlank()) ? req.getOfficeAddress() : fullAddr;
      e.setOfficeAddress(office);

      try {
        FileService.EncodedFile ef = fileService.encryptCertificationFile(certificationFile);
        e.setCertificationFile(ef.getOriginalName());
        e.setExpertEncoding(ef.getCipher());
        e.setExpertVector(ef.getIv());
      } catch (Exception ex) {
        throw new IOException("인증서 파일 암호화에 실패했습니다.", ex);
      }
      e.setExpertStatus(ExpertStatus.PENDING);
      expertRepository.save(e);
    } else {
      com.smhrd.dtect.entity.User u = new com.smhrd.dtect.entity.User();
      u.setMember(m);
      u.setUserTerms(req.getTermsAgree());
      userRepository.save(u);
    }
  }

  // 이메일 인증
  public void sendVerificationEmail(String toEmail) {
    final String key = toEmail.toLowerCase();
    String code = generateCode();
    verificationCodes.put(key, new VerificationInfo(code, System.currentTimeMillis()));

    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setTo(toEmail);
    msg.setSubject("회원가입 이메일 인증번호");
    msg.setText("인증번호는 [" + code + "] 입니다.\n\n5분 이내에 입력해 주세요.");
    mailSender.send(msg);
  }

  public boolean verifyEmailCode(String email, String code) {
    final String key = email.toLowerCase();
    VerificationInfo info = verificationCodes.get(key);
    if (info == null) return false;
    long elapsed = System.currentTimeMillis() - info.createdAtMs();
    if (elapsed > 5*60*1000L) { verificationCodes.remove(key); return false; }
    boolean ok = info.code().equals(code);
    if (ok) verificationCodes.remove(key);
    return ok;
  }

  private String generateCode() {
    var r = new java.security.SecureRandom();
    return Integer.toString(r.nextInt(900000)+100000);
  }
}
