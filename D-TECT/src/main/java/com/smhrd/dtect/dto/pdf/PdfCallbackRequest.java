package com.smhrd.dtect.dto.pdf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import org.antlr.v4.runtime.misc.NotNull;

/**
 * n8n에서 우리 서버로 호출하는 PDF 콜백 바디
 * - analResult: 배열/객체/문자열 어떤 형태도 JsonNode로 안전 수용
 * - reportPath가 비었으면 pdf[0].url을 대체 사용
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PdfCallbackRequest(
    String sid,
    @NotNull Long userId,
    String reportUrl,
    String analRate,
    @NotNull JsonNode analResult,
    List<PdfcoResultItem> pdf
) {}
