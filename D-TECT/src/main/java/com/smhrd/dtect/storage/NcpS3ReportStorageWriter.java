package com.smhrd.dtect.storage;

import com.smhrd.dtect.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.nio.file.FileSystems;
import java.time.Duration;
import java.time.LocalDate;

@Slf4j
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
                    .acl(ObjectCannedACL.PRIVATE) // 🔹 presigned URL로만 접근 가능
                    .build();

            s3.putObject(req, RequestBody.fromBytes(bytes));
            log.info("[NCP] uploaded bucket={} key={} size={}B contentType={}",
                    props.getBucket(), key, bytes.length, contentType);

        } catch (Exception e) {
            log.error("[NCP] upload failed bucket={} key={} → {}", props.getBucket(), key, e.getMessage(), e);
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

    /** 🔹 presigned URL 생성 (7일 유효) */
    public String generatePresignedUrl(String objectKey) {
        try (S3Presigner presigner = presigner()) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofDays(7)) // 🔹 7일 유효
                    .getObjectRequest(getObjectRequest)
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        }
    }
}
