package com.smhrd.dtect.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
    /** ncp | local */
    private String provider = "local";

    // Local 저장시
    private String localDir = "/home/git/uploads/reports"; // 쓰기 가능한 디렉터리

    // NCP S3 호환
    private String endpoint;     // https://kr.object.ncloudstorage.com
    private String region;       // kr-standard
    private String accessKey;
    private String secretKey;
    private String bucket;
    /** 공개 URL prefix (예: https://kr.object.ncloudstorage.com/<bucket>) */
    private String publicBaseUrl;
}
