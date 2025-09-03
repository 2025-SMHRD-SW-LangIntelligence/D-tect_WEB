//package com.smhrd.dtect.storage;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.web.client.RestTemplateBuilder;
//import org.springframework.http.*;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//
//import java.io.*;
//import java.net.URI;
//import java.nio.file.*;
//import java.time.Duration;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.UUID;
//
//@Component
//public class HttpReportStorageClient implements ReportStorageClient {
//
//    private final RestTemplate rt;
//    private final Path rootDir;
//
//    public HttpReportStorageClient(
//            RestTemplateBuilder builder,
//            @Value("${report.storage.local-root:./reports}") String localRoot,
//            @Value("${http.client.connect-timeout-ms:5000}") long connectTimeoutMs,
//            @Value("${http.client.read-timeout-ms:60000}") long readTimeoutMs
//    ) {
//        this.rt = builder
//                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
//                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
//                .additionalInterceptors((req, body, exec) -> {
//                    req.getHeaders().set(HttpHeaders.USER_AGENT, "D-TECT/1.0 (+httpclient)");
//                    return exec.execute(req, body);
//                })
//                .build();
//        this.rootDir = Paths.get(localRoot).toAbsolutePath().normalize();
//        try {
//            Files.createDirectories(this.rootDir);
//        } catch (IOException e) {
//            throw new IllegalStateException("Failed to create report root directory: " + this.rootDir, e);
//        }
//    }
//
//    /** http/https/file:/로부터 바이트 로드 + 로컬 경로도 지원 */
//    @Override
//    public byte[] loadBytes(String reportPath) {
//        if (reportPath == null || reportPath.isBlank()) {
//            throw new IllegalArgumentException("reportPath is null/blank");
//        }
//
//        // HTTP(S)
//        if (reportPath.startsWith("http://") || reportPath.startsWith("https://")) {
//            try {
//                HttpHeaders headers = new HttpHeaders();
//                headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_PDF_VALUE);
//                RequestEntity<Void> req = new RequestEntity<>(headers, HttpMethod.GET, URI.create(reportPath));
//                ResponseEntity<byte[]> res = rt.exchange(req, byte[].class);
//                if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
//                    return res.getBody();
//                }
//                throw new IllegalStateException("Failed to fetch pdf: " + res.getStatusCode());
//            } catch (Exception e) {
//                throw new IllegalStateException("HTTP fetch failed: " + e.getMessage(), e);
//            }
//        }
//
//        // file: 스킴
//        if (reportPath.startsWith("file:")) {
//            try {
//                Path p = Paths.get(URI.create(reportPath));
//                return Files.readAllBytes(p);
//            } catch (Exception e) {
//                throw new IllegalStateException("File fetch failed: " + reportPath, e);
//            }
//        }
//
//        // 로컬 경로 (절대/상대)
//        try {
//            Path p = Paths.get(reportPath);
//            if (!p.isAbsolute()) {
//                p = rootDir.resolve(p).normalize();
//            }
//            return Files.readAllBytes(p);
//        } catch (Exception e) {
//            throw new IllegalStateException("Local path fetch failed: " + reportPath, e);
//        }
//    }
//
//    /** 업로드: 입력 스트림을 로컬 파일로 저장하고 file: URL을 reportPath로 반환 */
//    @Override
//    public String save(String fileName, InputStream in, long size, String contentType) {
//        if (in == null) throw new IllegalArgumentException("input stream is null");
//
//        String safeName = sanitizeFileName(fileName != null ? fileName : "report.pdf");
//        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
//        String uuid = UUID.randomUUID().toString().replace("-", "");
//
//        Path dir = rootDir.resolve(datePath).normalize();
//        try {
//            Files.createDirectories(dir);
//            Path dest = dir.resolve(uuid + "_" + safeName);
//            try (OutputStream out = Files.newOutputStream(dest, StandardOpenOption.CREATE_NEW)) {
//                in.transferTo(out); // Java 9+
//            }
//            // file: 스킴으로 반환 (loadBytes에서 인식)
//            return dest.toAbsolutePath().toUri().toString(); // e.g. file:/.../reports/2025/08/27/uuid_report.pdf
//        } catch (IOException e) {
//            throw new IllegalStateException("Failed to save report to local storage", e);
//        }
//    }
//
//    /** 필요 시 외부 공개 URL이 있으면 반환(로컬 저장은 공개 URL 없음) */
//    @Override
//    public String resolvePublicUrl(String reportPath, boolean inline) {
//        return null; // 로컬 저장소는 공개 URL 미제공(컨트롤러가 프록시로 바이트를 내려줌)
//    }
//
//    // --- helpers ---
//    private static String sanitizeFileName(String name) {
//        // Windows 예약문자 제거
//        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
//    }
//}
