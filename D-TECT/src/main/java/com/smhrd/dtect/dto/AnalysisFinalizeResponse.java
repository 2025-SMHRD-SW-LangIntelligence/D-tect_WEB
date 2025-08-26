package com.smhrd.dtect.dto;

import com.smhrd.dtect.entity.AnalRate;

public record AnalysisFinalizeResponse(
    String sid,
    long totalItems,
    AnalRate analRate,
    boolean dispatched // PDF 웹훅 발사 성공 여부(스텁이면 true)
) {}
