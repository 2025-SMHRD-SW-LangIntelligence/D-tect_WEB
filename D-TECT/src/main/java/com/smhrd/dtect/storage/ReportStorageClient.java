package com.smhrd.dtect.storage;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 보고서(PDF) 저장소 추상화
 * - reportPath: 구현체가 반환/이용하는 저장소의 "정규 키" 또는 경로/URL (예: ncp://bucket/key, s3://..., file:/... 등)
 */
public interface ReportStorageClient {

    /** reportPath로 PDF 바이트 읽어오기 (프록시/다운로드 응답에 사용) */
    byte[] loadBytes(String reportPath);

    /**
     * PDF 업로드(필수 구현)
     * @param fileName    원본 파일명(확장자 포함 권장) – 저장 키 생성에 활용 가능
     * @param in          업로드 입력 스트림(호출측이 닫지 않음; 구현체가 try-with-resources로 닫아야 함)
     * @param size        총 바이트 수(모를 경우 -1 가능, 구현체에서 스트리밍 처리)
     * @param contentType 예: "application/pdf"
     * @return            저장된 보고서의 canonical reportPath (DB에 저장할 값)
     */
    String save(String fileName, InputStream in, long size, String contentType);

    /** (선택) 외부 공개/프리뷰용 URL이 있을 경우 반환. inline=true면 Content-Disposition=inline을 선호하는 형태로 제공 */
    default String resolvePublicUrl(String reportPath, boolean inline) { return null; }

    /** (선택) 일정 기간 유효한 사전서명 다운로드 URL 생성 */
    default String presignDownloadUrl(String reportPath, Duration ttl, boolean inline) { return null; }

    /** (선택) 스트리밍으로 읽고 싶을 때 사용. 기본은 전체 바이트를 메모리에 올려 InputStream으로 감쌈 */
    default InputStream openStream(String reportPath) {
        return new java.io.ByteArrayInputStream(loadBytes(reportPath));
    }

    /** (선택) 객체 삭제 */
    default boolean delete(String reportPath) { return false; }

    /** (선택) HEAD 메타데이터 조회 */
    default Optional<ObjectMeta> head(String reportPath) { return Optional.empty(); }

    /** (편의) 바이트 배열 업로드 오버로드 */
    default String save(String fileName, byte[] data, String contentType) {
        return save(fileName, new java.io.ByteArrayInputStream(data), data.length, contentType);
    }

    /** (선택) 메타데이터 타입 */
    final class ObjectMeta {
        private final long size;
        private final String contentType;
        private final String etag;
        private final Instant lastModified;

        public ObjectMeta(long size, String contentType, String etag, Instant lastModified) {
            this.size = size;
            this.contentType = contentType;
            this.etag = etag;
            this.lastModified = lastModified;
        }
        public long getSize() { return size; }
        public String getContentType() { return contentType; }
        public String getEtag() { return etag; }
        public Instant getLastModified() { return lastModified; }
    }
}
