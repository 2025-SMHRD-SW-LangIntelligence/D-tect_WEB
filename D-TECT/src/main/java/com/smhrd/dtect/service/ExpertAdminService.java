package com.smhrd.dtect.service;

import com.smhrd.dtect.dto.PendingExpertDetailDto;
import com.smhrd.dtect.dto.PendingExpertDto;
import com.smhrd.dtect.entity.*;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.FieldRepository;
import com.smhrd.dtect.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpertAdminService {

    private final ExpertRepository expertRepository;
    private final FieldRepository fieldRepository;
    private final MemberRepository memberRepository;

    @Value("${app.aes.secret}")
    private String aesSecret;

    private byte[] aesKey;

    @PostConstruct
    void initKey() throws Exception {
        this.aesKey = MessageDigest.getInstance("SHA-256")
                .digest(aesSecret.getBytes(StandardCharsets.UTF_8));
    }

    // 대기중 전문가 목록
    @Transactional(readOnly = true)
    public List<PendingExpertDto> listPending() {
        return expertRepository.findAllByExpertStatus(ExpertStatus.PENDING)
                .stream().map(PendingExpertDto::from).collect(Collectors.toList());
    }

    // 전문가 상세정보
    @Transactional(readOnly = true)
    public Optional<PendingExpertDetailDto> getDetail(Long expertIdx) {
        return expertRepository.findById(expertIdx).map(e -> {
            List<FieldName> fields = fieldRepository.findAllByExpert_ExpertIdx(expertIdx)
                    .stream().map(Field::getFieldName).toList();
            boolean hasCert = (e.getExpertEncoding() != null && e.getExpertVector() != null);
            return new PendingExpertDetailDto(
                    e.getExpertIdx(),
                    e.getMember() != null ? e.getMember().getName() : null,
                    e.getOfficeName(),
                    e.getOfficeAddress(),
                    e.getMember() != null ? e.getMember().getJoinedAt() : null,
                    e.getExpertStatus(),
                    fields,
                    e.getCertificationFile(),
                    hasCert
            );
        });
    }

    // 대기중 전체 + 분야 묶어오기
    @Transactional(readOnly = true)
    public List<PendingExpertDetailDto> listPendingWithDetails() {
        List<Expert> experts = expertRepository.findAllByExpertStatus(ExpertStatus.PENDING);
        if (experts.isEmpty()) return List.of();

        List<Long> ids = experts.stream().map(Expert::getExpertIdx).toList();
        List<Field> fields = fieldRepository.findAllByExpert_ExpertIdxIn(ids);
        Map<Long, List<FieldName>> fieldMap = fields.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getExpert().getExpertIdx(),
                        Collectors.mapping(Field::getFieldName, Collectors.toList())
                ));

        return experts.stream().map(e -> {
            boolean hasCert = (e.getExpertEncoding() != null && e.getExpertVector() != null);
            return new PendingExpertDetailDto(
                    e.getExpertIdx(),
                    e.getMember() != null ? e.getMember().getName() : null,
                    e.getOfficeName(),
                    e.getOfficeAddress(),
                    e.getMember() != null ? e.getMember().getJoinedAt() : null,
                    e.getExpertStatus(),
                    fieldMap.getOrDefault(e.getExpertIdx(), List.of()),
                    e.getCertificationFile(),
                    hasCert
            );
        }).toList();
    }

    // 승인
    @Transactional
    public void approve(Long expertIdx) {
        Expert expert = expertRepository.findById(expertIdx)
                .orElseThrow(() -> new IllegalArgumentException("전문가 없음: " + expertIdx));

        // 자격 증명 필수
        if (expert.getExpertEncoding() == null || expert.getExpertVector() == null) {
            throw new IllegalStateException("자격증명 파일이 없습니다.");
        }

        expert.setExpertStatus(ExpertStatus.APPROVED);
        expertRepository.save(expert);

        // 멤버 역할 전환
        Member member = expert.getMember();
        if (member != null && member.getMemRole() != MemRole.EXPERT) {
            member.setMemRole(MemRole.EXPERT);
            memberRepository.save(member);
        }
    }

    // 거절 (분야/전문가/멤버까지 정리)
    @Transactional
    public void reject(Long expertIdx) {
        Expert e = expertRepository.findById(expertIdx)
                .orElseThrow(() -> new IllegalArgumentException("전문가 없음: " + expertIdx));

        fieldRepository.deleteByExpert_ExpertIdx(expertIdx);
        expertRepository.deleteById(expertIdx);

        Long memIdx = (e.getMember() != null) ? e.getMember().getMemIdx() : null;
        if (memIdx != null) {
            memberRepository.deleteById(memIdx);
        }
    }

    // AES
    private static final String TF = "AES/CBC/PKCS5Padding";

    private static byte[] decrypt(byte[] cipher, byte[] key, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance(TF);
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return c.doFinal(cipher);
    }

    // 증빙파일 다운로드(복호화)
    @Transactional(readOnly = true)
    public byte[] downloadCertificate(Long expertIdx) {
        return expertRepository.findById(expertIdx).map(e -> {
            byte[] cipher = e.getExpertEncoding();
            byte[] iv     = e.getExpertVector();
            if (cipher == null || iv == null || iv.length != 16) return null;
            try { return decrypt(cipher, aesKey, iv); } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    // 증빙 원본 파일 명
    @Transactional(readOnly = true)
    public String getCertificateFilename(Long expertIdx) {
        return expertRepository.findById(expertIdx)
                .map(Expert::getCertificationFile).orElse("certificate.bin");
    }

}
