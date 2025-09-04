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
	
	    /**
	     * 파일 업로드 (Content-Disposition → attachment 만 넣음)
	     */
	    @Override
	    public String upload(String objectName, byte[] bytes, String contentType) {
	        if (objectName == null || objectName.isBlank()) {
	            throw new IllegalArgumentException("objectName must not be blank");
	        }

	        String key = objectName.replace(FileSystems.getDefault().getSeparator(), "/");
	        String filename = key.substring(key.lastIndexOf("/") + 1);

	        try (S3Client s3 = s3()) {
	            // RFC 5987 방식으로 UTF-8 안전하게 인코딩된 파일명
	            String encoded = java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8)
	                    .replace("+", "%20");
	            String contentDisp = "attachment; filename*=UTF-8''" + encoded;

	            PutObjectRequest req = PutObjectRequest.builder()
	                    .bucket(props.getBucket())
	                    .key(key)
	                    .contentType(contentType != null ? contentType : "application/pdf")
	                    .contentDisposition(contentDisp) // 안전하게 파일명 보존
	                    .acl(ObjectCannedACL.PUBLIC_READ)
	                    .build();

	            s3.putObject(req, RequestBody.fromBytes(bytes));
	            log.info("[NCP] uploaded bucket={} key={} size={}B filename={} contentType={} (public-read)",
	                    props.getBucket(), key, bytes.length, filename, contentType);

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
	
	    /** Presigned URL (7일 유효) */
	    public String generatePresignedUrl(String objectKey) {
	        try (S3Presigner presigner = S3Presigner.builder()
	                .endpointOverride(URI.create(props.getEndpoint()))
	                .region(Region.of(props.getRegion()))
	                .credentialsProvider(StaticCredentialsProvider.create(
	                        AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
	                ))
	                .build()) {

	            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
	                    .bucket(props.getBucket())
	                    .key(objectKey)
	                    .build();

	            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
	                    .signatureDuration(Duration.ofDays(7)) // 다시 7일 유효
	                    .getObjectRequest(getObjectRequest)
	                    .build();

	            String url = presigner.presignGetObject(presignRequest).url().toString();
	            log.debug("[NCP] presigned URL generated key={} url={}", objectKey, url);
	            return url;

	        } catch (Exception e) {
	            log.error("[NCP] presigned URL 생성 실패 key={} → {}", objectKey, e.getMessage(), e);
	            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage(), e);
	        }
	    }
	
	    /** objectKey로부터 PDF 바이트 로드 */
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
	            log.error("[NCP] loadBytes 실패 key={} → {}", objectKey, e.getMessage(), e);
	            return null;
	        }
	    }
	}
