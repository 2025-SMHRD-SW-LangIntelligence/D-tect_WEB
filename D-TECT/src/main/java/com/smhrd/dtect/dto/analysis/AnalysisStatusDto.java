package com.smhrd.dtect.dto.analysis;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 분석 진행 상태 응답 DTO
 * - received : (선택) 들어온 총 입력 수 (모델 push면 생략 가능)
 * - processed: 누적 처리된 결과 수
 * - total    : (선택) 전체 예상 수(미정이면 null)
 * - progress : (파생) total이 있을 때만 0~100%
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisStatusDto {

    private long received;
    private long processed;
    private Long total; // nullable

    public AnalysisStatusDto() {}

    public AnalysisStatusDto(long received, long processed, Long total) {
        this.received = received;
        this.processed = processed;
        this.total = total;
    }

    public long getReceived() { return received; }
    public long getProcessed() { return processed; }
    public Long getTotal() { return total; }

    /** total이 있을 때만 진행률(%) 제공. 없으면 null */
    public Integer getProgress() {
        if (total == null || total <= 0) return null;
        long p = Math.min(processed, total);
        return (int) Math.round(p * 100.0 / Math.max(total, 1));
    }
}
