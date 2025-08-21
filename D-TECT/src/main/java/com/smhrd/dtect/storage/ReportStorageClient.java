package com.smhrd.dtect.storage;

public interface ReportStorageClient {

    // reportPath로 PDF 바이트 읽어오기
    byte[] loadBytes(String reportPath);

    // 프리뷰, 다운로드용 외부 URL이 있다면 반환
    default String resolvePublicUrl(String reportPath, boolean inline) { return null; }
}
