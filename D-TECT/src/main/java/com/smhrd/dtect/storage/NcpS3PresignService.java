// src/main/java/com/smhrd/dtect/storage/NcpS3PresignService.java
package com.smhrd.dtect.storage;

import com.smhrd.dtect.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class NcpS3PresignService {

    private final StorageProperties props;

    private S3Presigner presigner() {
        return S3Presigner.builder()
                .region(Region.of(props.getRegion()))
                .endpointOverride(URI.create(props.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    /** 기존 시그니처(하위호환) — 기본 파일명 사용 */
    public String presignGet(String reportUrlOrKey, Duration ttl, boolean inline) {
        return presignGet(reportUrlOrKey, ttl, inline, null);
    }

    /**
     * 사용자명 기반 한글 파일명 포함 Presigned URL 생성
     * 예) "wfos1234의 사이버불링 탐지 보고서.pdf"
     */
    public String presignGet(String reportUrlOrKey, Duration ttl, boolean inline, String username) {
        String key = extractKey(reportUrlOrKey);
        String displayName = buildDisplayName(username); // 한글 파일명

        try (S3Presigner p = presigner()) {
            GetObjectRequest req = GetObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .responseContentType("application/pdf")
                    .responseContentDisposition(contentDisposition(inline, displayName))
                    .build();

            PresignedGetObjectRequest pre = p.presignGetObject(b -> b
                    .signatureDuration(ttl)
                    .getObjectRequest(req));
            return pre.url().toString();
        }
    }

    /** "username의 사이버불링 탐지 보고서.pdf" 생성 (username 없으면 기본값) */
    private String buildDisplayName(String username) {
        if (username == null || username.isBlank()) {
            return "사이버불링 탐지 보고서.pdf";
        }
        return username + "의 사이버불링 탐지 보고서.pdf";
    }

    /** Content-Disposition: filename + filename* (UTF-8) */
    private String contentDisposition(boolean inline, String filename) {
        String fallback = toAsciiFilename(filename); // 구형 브라우저 fallback
        String encoded  = urlEncodeUtf8(filename);   // RFC 5987
        return (inline ? "inline" : "attachment")
                + "; filename=\"" + fallback + "\""
                + "; filename*=UTF-8''" + encoded;
    }

    private static String urlEncodeUtf8(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** 비ASCII 제거한 안전한 대체 파일명 */
    private static String toAsciiFilename(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_")
                .replaceAll("[^\\x20-\\x7E]", "");
        if (n.isBlank()) n = "report.pdf";
        return n;
    }

    /** DB에 URL이 저장돼 있어도 key만 추출 */
    private String extractKey(String s) {
        if (s == null) return null;
        String base = props.getPublicBaseUrl();
        if (base != null && !base.isBlank()) {
            base = base.replaceAll("/+$", "");
            if (s.startsWith(base + "/")) return s.substring(base.length() + 1);
        }
        // 대략 https://host/<bucket>/ 이하를 key로
        return s.replaceFirst("^https?://[^/]+/[^/]+/", "");
    }
}
