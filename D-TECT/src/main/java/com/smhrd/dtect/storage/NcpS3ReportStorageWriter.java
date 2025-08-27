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
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "ncp")
@Slf4j
public class NcpS3ReportStorageWriter implements ReportStorageWriter {

    private final StorageProperties props;

    private S3Client s3() {
        validateProps();
        return S3Client.builder()
                .region(Region.of(props.getRegion())) // e.g. "kr-standard"
                .endpointOverride(URI.create(props.getEndpoint())) // e.g. https://kr.object.ncloudstorage.com
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true) // NCP는 path-style 권장
                        .build())
                .build();
    }

    @Override
    public String upload(String objectName, byte[] bytes, String contentType) throws Exception {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("objectName must not be blank");
        }
        if (bytes == null) {
            throw new IllegalArgumentException("bytes must not be null");
        }

        try (S3Client s3 = s3()) {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(objectName)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build();
            s3.putObject(req, RequestBody.fromBytes(bytes));
            log.info("[NCP] uploaded bucket={} key={} size={}B", props.getBucket(), objectName, bytes.length);
        }
        // DB에는 "키"만 저장 (공개 URL 필요 시 props.publicBaseUrl + "/" + objectName 사용)
        return objectName;
    }

    private void validateProps() {
        if (isBlank(props.getEndpoint())
                || isBlank(props.getRegion())
                || isBlank(props.getAccessKey())
                || isBlank(props.getSecretKey())
                || isBlank(props.getBucket())) {
            throw new IllegalStateException("NCP storage properties are missing. " +
                    "required=endpoint,region,accessKey,secretKey,bucket");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
