package com.smhrd.dtect.dto;

public record PdfcoResultItem(
    int pageCount,
    String url,                 // 서명된 S3 링크
    String outputLinkValidTill, // "2025-08-26T09:33:24.869055+00:00"
    int duration,               // ms 또는 ms 유사값
    String name                 // "htmltopdf.pdf"
) {}
