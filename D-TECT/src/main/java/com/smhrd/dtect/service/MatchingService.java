package com.smhrd.dtect.service;

import com.smhrd.dtect.entity.*;
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
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatchingRepository matchingRepository;
    private final ExpertRepository expertRepository;
    private final UserRepository userRepository;

    @Value("${app.aes.secret}")
    private String aesSecret;

    private byte[] aesKey;

    @PostConstruct
    void init() throws Exception {
        this.aesKey = MessageDigest.getInstance("SHA-256")
                .digest(aesSecret.getBytes(StandardCharsets.UTF_8));
    }

    // 전문가 리스트(승인된 사람만)
    public List<Expert> listExperts() {
        return expertRepository.findAllByExpertStatus(ExpertStatus.APPROVED);
    }

    // 매칭 요청(첨부 1개 옵션)
    public Matching request(Long userId, Long expertId, String message, MultipartFile attachment) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음: " + userId));
        Expert expert = expertRepository.findById(expertId)
                .orElseThrow(() -> new IllegalArgumentException("전문가 없음: " + expertId));

        Matching m = new Matching();
        m.setUser(user);
        m.setExpert(expert);
        m.setRequestMessage(message);
        m.setStatus(MatchingStatus.PENDING);
        m.setRequestedAt(new Timestamp(System.currentTimeMillis()));
        m.setIsActive(true);

        if (attachment != null && !attachment.isEmpty()) {
            byte[] iv = randomIv();
            byte[] cipher = encrypt(attachment.getBytes(), aesKey, iv);
            m.setMatchingEncoding(cipher);
            m.setMatchingVector(iv);
            m.setMatchingFile(attachment.getOriginalFilename());
        }

        return matchingRepository.save(m);
    }

    public void approve(Long matchingId) {
        Matching m = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new IllegalArgumentException("매칭 없음: " + matchingId));
        m.setStatus(MatchingStatus.APPROVED);
        m.setApprovedAt(new Timestamp(System.currentTimeMillis()));
        m.setIsActive(true);
        matchingRepository.save(m);
    }

    public void reject(Long matchingId) {
        Matching m = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new IllegalArgumentException("매칭 없음: " + matchingId));
        m.setStatus(MatchingStatus.REJECTED);
        m.setRejectedAt(new Timestamp(System.currentTimeMillis()));
        m.setIsActive(false);
        matchingRepository.save(m);
    }

    public Optional<Matching> get(Long id) {
        return matchingRepository.findById(id);
    }

    public List<Matching> expertInbox(Long expertId) {
        return matchingRepository.findByExpert_ExpertIdxAndStatus(expertId, MatchingStatus.PENDING);
    }

    public byte[] downloadInitialAttachment(Long matchingId) {
        return matchingRepository.findById(matchingId).map(m -> {
            byte[] c = m.getMatchingEncoding();
            byte[] iv = m.getMatchingVector();
            if (c == null || iv == null || iv.length != 16) return null;
            try { return decrypt(c, aesKey, iv); } catch (Exception e) { return null; }
        }).orElse(null);
    }

    public byte[] downloadAttachment(Long matchingId) {
        return downloadInitialAttachment(matchingId);
    }

    // AES
    private static final String TF = "AES/CBC/PKCS5Padding";

    private static byte[] randomIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
    private static byte[] encrypt(byte[] plain, byte[] key, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance(TF);
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return c.doFinal(plain);
    }
    private static byte[] decrypt(byte[] cipher, byte[] key, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance(TF);
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return c.doFinal(cipher);
    }
}
