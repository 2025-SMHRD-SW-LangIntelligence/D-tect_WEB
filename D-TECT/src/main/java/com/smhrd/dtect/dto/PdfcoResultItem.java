package com.smhrd.dtect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * n8n HTML→PDF 노드(또는 유사 PDF 서비스)에서 반환하는 단일 파일 메타
 * - 박싱 타입(Integer)로 null-safe
 * - 알 수 없는 필드는 무시
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PdfcoResultItem(
    @JsonProperty("pageCount") Integer pageCount,
    @JsonProperty("url") String url,                 // 서명된 S3 링크(또는 임시 다운로드 URL)
    @JsonProperty("outputLinkValidTill") String outputLinkValidTill, // ISO-8601
    @JsonProperty("duration") Integer duration,      // ms 또는 유사
    @JsonProperty("name") String name                // "htmltopdf.pdf"
) {}
