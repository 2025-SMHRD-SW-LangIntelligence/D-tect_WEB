package com.smhrd.dtect.service;

import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.Upload;
import com.smhrd.dtect.entity.UploadFile;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.UploadFileRepository;
import com.smhrd.dtect.repository.UploadRepository;
import com.smhrd.dtect.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLConnection;
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
    private final UserRepository userRepository;
    private final ExpertRepository expertRepository;

    @Value("${app.aes.secret}")
    private String aesSecret;

    private byte[] aesKey; // SHA-256(pathfinder) 결과(32바이트)

    // 브라우저 파일을 어떻게 다룰지 결정하는 부분
    // 미리보기 등등 가능하게 해줌
    public static String guessContentType(String filename) {
        String type = URLConnection.guessContentTypeFromName(filename);
        return (type != null) ? type : "application/octet-stream";
    }

    @PostConstruct
    void initKey() throws Exception {
        this.aesKey = MessageDigest.getInstance("SHA-256")
                .digest(aesSecret.getBytes(StandardCharsets.UTF_8));
    }

    // 한 번의 업로드에 여러 파일 저장
    public Upload uploadFiles(Long userIdx, Long expertIdx, List<MultipartFile> files) throws Exception {
        User user = userRepository.findById(userIdx)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userIdx));
        Expert expert = expertRepository.findById(expertIdx)
                .orElseThrow(() -> new IllegalArgumentException("전문가를 찾을 수 없습니다: " + expertIdx));

        Upload upload = new Upload();
        upload.setUser(user);
        upload.setExpert(expert);
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
            uf.setUploadEncoding(cipher); // 파일별 암호문
            uf.setUploadVector(iv);       // 파일별 벡터

            uploadFileRepository.save(uf);
        }
        return saved;
    }

    // 사용자-전문가 매칭 업로드 목록
    public List<Upload> list(Long userIdx, Long expertIdx) {
        return uploadRepository.findWithFiles(userIdx, expertIdx);
    }

    // 파일 단건 복호화 & 다운로드
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

    // 암복호화 유틸

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
