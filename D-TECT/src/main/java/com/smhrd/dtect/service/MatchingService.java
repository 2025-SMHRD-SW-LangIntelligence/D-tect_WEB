package com.smhrd.dtect.service;

import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.Matching;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.MatchingRepository;
import com.smhrd.dtect.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatchingRepository matchingRepository;
    private final UserRepository userRepository;
    private final ExpertRepository expertRepository;

    @Value("${app.aes.secret}")
    private String aesSecret;

    private byte[] aesKey;

    @PostConstruct
    void initKey() throws Exception {
        this.aesKey = MessageDigest.getInstance("SHA-256")
                .digest(aesSecret.getBytes(StandardCharsets.UTF_8));
    }

    // 매칭 요청 및 첨부 파일 1개(선택)
    public Matching request(Long userIdx, Long expertIdx, String message, MultipartFile attachment) throws Exception {
        User user = userRepository.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userIdx));
        Expert expert = expertRepository.findById(expertIdx)
                .orElseThrow(() -> new IllegalArgumentException("전문가를 찾을 수 없습니다: " + expertIdx));

        Matching m = new Matching();
        m.setUser(user);
        m.setExpert(expert);
        m.setRequestMessage(message);
        // Matching 테이블에서
        // @PrePersist 가 status=PENDING, requestedAt, isActive 세팅

        if (attachment != null && !attachment.isEmpty()) {
            byte[] iv     = randomIv();
            byte[] cipher = encryptAesCbcPkcs5(attachment.getBytes(), aesKey, iv);

            m.setMatchingFile(attachment.getOriginalFilename());  // 파일명
            m.setMatchingEncoding(cipher);                        // 암호문
            m.setMatchingVector(iv);                              // IV(16바이트)
        }

        return matchingRepository.save(m);
    }

    // 매칭때 업로드한 파일 1개 복호화
    public byte[] downloadAttachment(Long matchingIdx) {
        return matchingRepository.findById(matchingIdx).map(m -> {
            byte[] cipher = m.getMatchingEncoding();
            byte[] iv     = m.getMatchingVector();
            if (cipher == null || iv == null || iv.length != 16) return null;
            try {
                return decryptAesCbcPkcs5(cipher, aesKey, iv);
            } catch (Exception e) {
                return null;
            }
        }).orElse(null);
    }

    // AES
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private static byte[] randomIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
    private static byte[] encryptAesCbcPkcs5(byte[] plain, byte[] key, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance(TRANSFORMATION);
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return c.doFinal(plain);
    }
    private static byte[] decryptAesCbcPkcs5(byte[] cipher, byte[] key, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance(TRANSFORMATION);
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return c.doFinal(cipher);
    }
}
