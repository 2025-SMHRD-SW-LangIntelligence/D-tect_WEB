package com.smhrd.dtect.storage;

public interface ReportStorageWriter {
    /**
     * @param objectName 저장할 객체 키(예: reports/2025/08/ABC123.pdf)
     * @param bytes      파일 바이트
     * @param contentType 예: application/pdf
     * @return 저장된 객체의 "키" (DB에는 이 값을 저장) 또는 공개 URL(원하면)
     */
    String upload(String objectName, byte[] bytes, String contentType) throws Exception;

    /** base path 없이 파일명만 들어오면 내부 규칙으로 objectName 만들어 저장 */
    default String uploadAutoName(String sid, String originalName, byte[] bytes, String contentType) throws Exception {
        String safeName = (originalName == null || originalName.isBlank()) ? "report.pdf" : originalName;
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
        String objectName = String.format("reports/%04d/%02d/%s-%d-%s",
                now.getYear(), now.getMonthValue(), sid, System.currentTimeMillis(), safeName);
        return upload(objectName, bytes, contentType);
    }
}
