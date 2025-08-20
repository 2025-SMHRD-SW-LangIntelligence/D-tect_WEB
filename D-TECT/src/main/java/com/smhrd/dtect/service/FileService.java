package com.smhrd.dtect.service;

import com.smhrd.dtect.entity.Matching;
import com.smhrd.dtect.entity.Upload;
import com.smhrd.dtect.entity.UploadFile;
import com.smhrd.dtect.repository.MatchingRepository;
import com.smhrd.dtect.repository.UploadFileRepository;
import com.smhrd.dtect.repository.UploadRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    private final UploadRepository uploadRepository;
    private final UploadFileRepository uploadFileRepository;
    private final MatchingRepository matchingRepository;

    @Value("${app.aes.secret}")
    private String aesSecret;

    private byte[] aesKey; // SHA-256(pathfinder) 결과(32바이트)

    @PostConstruct
    void initKey() throws Exception {
        this.aesKey = MessageDigest.getInstance("SHA-256")
                .digest(aesSecret.getBytes(StandardCharsets.UTF_8));
    }

    // 한 매칭에 여러파일 업로드
    public Upload uploadFiles(Long matchingId, List<MultipartFile> files) throws Exception {
        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new IllegalArgumentException("매칭을 찾을 수 없습니다: " + matchingId));

        Upload upload = new Upload();
        upload.setMatching(matching);
        upload.setCreatedAt(Timestamp.from(Instant.now()));
        Upload saved = uploadRepository.save(upload);

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            byte[] plain  = file.getBytes();
            byte[] iv     = randomIv();
            byte[] cipher = encryptAesCbcPkcs5(plain, aesKey, iv);

            UploadFile uf = new UploadFile();
            uf.setUpload(saved);
            uf.setFileName(file.getOriginalFilename());
            uf.setUploadEncoding(cipher);
            uf.setUploadVector(iv);

            uploadFileRepository.save(uf);
        }
        return saved;
    }

    // 매칭 기준 업로드 목록
    public List<Upload> listByMatching(Long matchingId) {
        return uploadRepository.findWithFiles(matchingId);
    }

    // 복호화
    public byte[] downloadFilePlain(Long fileIdx) {
        return uploadFileRepository.findById(fileIdx).map(f -> {
            byte[] cipher = f.getUploadEncoding();
            byte[] iv     = f.getUploadVector();
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
    
    // 기존 AES 암호화 업로드와 별개로 일반 파일 업로드용
    public String saveFileToServer(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String savedFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        File dest = new File("/home/git/uploads", savedFileName); // upload-dir로 변경 가능
        file.transferTo(dest);

        return savedFileName;
    }

}
