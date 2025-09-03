package com.smhrd.dtect.storage;

import com.smhrd.dtect.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportUrlBuilder {

    private final StorageProperties props;

    /** objectKey → 공개 URL (provider에 따라) */
    public String toPublicUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return null;
        String provider = String.valueOf(props.getProvider()).toLowerCase();
        if ("ncp".equals(provider)) {
            String base = trimRightSlash(props.getPublicBaseUrl());
            return (base != null) ? base + "/" + objectKey : null;
        }
        if ("local".equals(provider)) {
            return "/uploads/" + objectKey.replace('\\', '/'); // 정적 서빙 규칙과 일치 필요
        }
        return null;
    }

    private static String trimRightSlash(String s) {
        if (s == null || s.isBlank()) return null;
        return s.replaceAll("/+$", "");
    }
}
