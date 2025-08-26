package com.smhrd.dtect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 진행 상태 응답 DTO
 * progress : 0~100 퍼센트
 * received : 입력 프레임수(옵션)
 * processed: 처리 완료 건수(items.size)
 * total    : 전체 예상 건수(알 수 없으면 processed와 동일하게 세팅됨)
 * ageSec   : 마지막 업데이트로부터 경과 초
 * done     : 세션 종료/미존재 등으로 더 이상 진행 안 함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisStatusDto {
    private String sid;
    private int progress;
    private long received;
    private long processed;
    private int total;
    private int ageSec;
    private boolean done;
}