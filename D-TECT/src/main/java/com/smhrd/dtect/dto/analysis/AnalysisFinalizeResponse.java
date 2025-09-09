package com.smhrd.dtect.dto.analysis;

import com.smhrd.dtect.entity.analysis.AnalRate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 최종 집계/등급 + PDF 웹훅 전송 결과 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisFinalizeResponse {
    private String sid;
    private int itemCount;
    private AnalRate rate;          // NORMAL / WARNING / DANGER
    private boolean pdfDispatched;  // PDF 웹훅 호출 성공 여부
}