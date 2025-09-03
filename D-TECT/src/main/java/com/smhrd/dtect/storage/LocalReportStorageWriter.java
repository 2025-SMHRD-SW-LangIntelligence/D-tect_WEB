package com.smhrd.dtect.storage;

import com.smhrd.dtect.config.StorageProperties;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.storage",
        name = "provider",
        havingValue = "local",
        matchIfMissing = true // 설정이 없으면 local로
)
public class LocalReportStorageWriter implements ReportStorageWriter {

    private final StorageProperties props;

    @Override
    public String upload(String objectName, byte[] bytes, String contentType) throws Exception {
        Path base = Path.of(props.getLocalDir()).toAbsolutePath().normalize();
        Path target = base.resolve(objectName).normalize();

        if (!target.startsWith(base)) throw new IllegalArgumentException("Invalid objectName path outside base dir");

        Files.createDirectories(target.getParent());
        Files.write(target, bytes);

        // 로컬은 "상대 키"를 그대로 반환 → Analysis.reportPath 에 저장
        return objectName.replace(File.separatorChar, '/');
    }
}
