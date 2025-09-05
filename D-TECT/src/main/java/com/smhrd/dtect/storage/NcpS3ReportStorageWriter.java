package com.smhrd.dtect.storage;

import com.smhrd.dtect.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.nio.file.FileSystems;
import java.time.Duration;
import java.time.LocalDate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "ncp")
public class NcpS3ReportStorageWriter implements ReportStorageWriter {

    private final StorageProperties props;

    private S3Client s3() {
        return S3Client.builder()
                .endpointOverride(URI.create(props.getEndpoint()))
                .region(Region.of(props.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
                ))
                .build();
    }

    private S3Presigner presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(props.getEndpoint()))
                .region(Region.of(props.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
                ))
                .build();
    }

    @Override
    public String upload(String objectName, byte[] bytes, String contentType) {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("objectName must not be blank");
        }
        String key = objectName.replace(FileSystems.getDefault().getSeparator(), "/");
        try (S3Client s3 = s3()) {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .contentType(contentType != null ? contentType : "application/pdf")
                    // dev 정책 유지: 공개 금지, presigned로만 접근
                    .acl(ObjectCannedACL.PRIVATE)
                    .build();

            s3.putObject(req, RequestBody.fromBytes(bytes));

        } catch (Exception e) {
            throw new RuntimeException("NCP upload failed: " + e.getMessage(), e);
        }
        return key;
    }

    @Override
    public String uploadAutoName(String owner, String originalName, byte[] bytes, String contentType) {
        String datePath = LocalDate.now().toString();
        String baseName = (owner != null ? owner : "anon") + "-" +
                (originalName != null ? originalName : "report.pdf");
        String key = "reports/" + datePath + "/" + baseName;
        return upload(key, bytes, contentType);
    }

    /** presigned URL 생성 (7일 유효) — dev 동작 유지 */
    // 기존 1-인자 메서드는 유지(호환)
    public String generatePresignedUrl(String objectKey) {
        return generatePresignedUrl(objectKey, null);
    }

    // ★ 새 오버로드: 다운로드 파일명 지정까지
    public String generatePresignedUrl(String objectKey, String downloadName) {
        try (S3Presigner presigner = presigner()) {
            GetObjectRequest.Builder get = GetObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(objectKey);

            if (downloadName != null && !downloadName.isBlank()) {
                String encoded = URLEncoder.encode(downloadName, StandardCharsets.UTF_8)
                        .replace("+", "%20");
                get = get.responseContentDisposition("attachment; filename*=UTF-8''" + encoded);
            }

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofDays(7))
                    .getObjectRequest(get.build())
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage(), e);
        }
    }

    /** 팀원 기능 흡수: S3에서 바이트 바로 읽기(비공개 객체도 자격증명으로 접근 가능) */
    public byte[] loadBytes(String objectKey) {
        try (S3Client s3 = s3()) {
            ResponseBytes<GetObjectResponse> obj = s3.getObject(
                    GetObjectRequest.builder()
                            .bucket(props.getBucket())
                            .key(objectKey)
                            .build(),
                    ResponseTransformer.toBytes()
            );
            return obj.asByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
