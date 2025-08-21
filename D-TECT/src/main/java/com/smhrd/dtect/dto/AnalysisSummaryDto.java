package com.smhrd.dtect.dto;

import com.smhrd.dtect.entity.AnalRate;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.Timestamp;

@Getter
@AllArgsConstructor
public class AnalysisSummaryDto {
    private Long analIdx;
    private String fileName;
    private Timestamp createdAt;
    private AnalRate analRate;
    private String previewUrl;
    private String downloadUrl;
}
