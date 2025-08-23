package com.smhrd.dtect.service;

import com.smhrd.dtect.dto.ExpertMatchingSummaryDto;
import com.smhrd.dtect.dto.UserMatchingSummaryDto;
import com.smhrd.dtect.entity.*;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.MatchingRepository;
import com.smhrd.dtect.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatchingRepository matchingRepository;
    private final ExpertRepository expertRepository;
    private final UserRepository userRepository;

    private static final java.util.Set<String> ALLOWED_REASONS = Set.of(
            "VIOLENCE","DEFAMATION","STALKING","SEXUAL","LEAK","BULLYING","CHANTAGE","EXTORTION"
    );

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
    @Transactional
    public Matching request(Long userId, Long expertId, String message, String requestReason, MultipartFile attachment) throws Exception {
        if (requestReason == null || !ALLOWED_REASONS.contains(requestReason)) {
            throw new IllegalArgumentException("유효하지 않은 상담 유형입니다.");
        }

        boolean dup = matchingRepository.existsByUser_UserIdxAndExpert_ExpertIdxAndIsActiveTrue(userId, expertId)
                || matchingRepository.existsByUser_UserIdxAndExpert_ExpertIdxAndStatusIn(
                userId, expertId, List.of(MatchingStatus.PENDING, MatchingStatus.APPROVED));
        if (dup) {
            throw new IllegalStateException("이미 진행 중인 상담이 있어 새로 신청할 수 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음: " + userId));
        Expert expert = expertRepository.findById(expertId)
                .orElseThrow(() -> new IllegalArgumentException("전문가 없음: " + expertId));

        Matching m = new Matching();
        m.setUser(user);
        m.setExpert(expert);
        m.setRequestMessage(message);
        m.setRequestReason(requestReason);
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

    @Transactional
    public void approve(Long matchingId) {
        Matching m = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new IllegalArgumentException("매칭 없음: " + matchingId));
        if (m.getStatus() != MatchingStatus.PENDING)
            throw new IllegalStateException("승인은 대기(PENDING) 상태에서만 가능합니다.");

        m.setStatus(MatchingStatus.APPROVED);
        m.setApprovedAt(new Timestamp(System.currentTimeMillis()));
        m.setIsActive(true);
        matchingRepository.save(m);
    }

    @Transactional
    public void reject(Long matchingId) {
        Matching m = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new IllegalArgumentException("매칭 없음: " + matchingId));
        if (m.getStatus() != MatchingStatus.PENDING)
            throw new IllegalStateException("거절은 대기(PENDING) 상태에서만 가능합니다.");

        m.setStatus(MatchingStatus.REJECTED);
        m.setRejectedAt(new Timestamp(System.currentTimeMillis()));
        m.setIsActive(false);
        matchingRepository.save(m);
    }

    @Transactional
    public void complete(Long matchingId) {
        Matching m = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new IllegalArgumentException("매칭 없음: " + matchingId));
        if (m.getStatus() != MatchingStatus.APPROVED)
            throw new IllegalStateException("완료는 승인(APPROVED) 후에만 가능합니다.");
        m.setStatus(MatchingStatus.COMPLETED);
        m.setCompletedAt(new Timestamp(System.currentTimeMillis()));
        m.setIsActive(false);
        matchingRepository.save(m);
    }

    @Transactional
    public void cancel(Long matchingId) {
        Matching m = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new IllegalArgumentException("매칭 없음: " + matchingId));
        if (m.getStatus() == MatchingStatus.COMPLETED || m.getStatus() == MatchingStatus.REJECTED)
            throw new IllegalStateException("완료/거절된 건은 취소할 수 없습니다.");
        m.setStatus(MatchingStatus.CANCELED);
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

    public List<UserMatchingSummaryDto> getUserMatchings(Long userIdx) {
        return matchingRepository.findUserSummaries(userIdx);
    }

    public List<ExpertMatchingSummaryDto> getExpertMatchings(Long expertIdx) {
        return matchingRepository.findExpertSummaries(expertIdx);
    }
}
