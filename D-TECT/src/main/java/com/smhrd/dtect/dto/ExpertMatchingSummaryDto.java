package com.smhrd.dtect.dto;

import com.smhrd.dtect.entity.MatchingStatus;
import lombok.Getter;

import java.sql.Timestamp;

@Getter
public class ExpertMatchingSummaryDto {
    private Long matchingIdx;
    private Timestamp requestedAt;   // 신청 시각
    private String customerName;     // 고객명
    private String requestReason;    // 분류
    private Timestamp matchedAt;     // 매칭일(= approvedAt)
    private MatchingStatus status;   // 매칭여부 판단용

    public ExpertMatchingSummaryDto(Long matchingIdx,
                                    Timestamp requestedAt,
                                    String customerName,
                                    String requestReason,
                                    Timestamp matchedAt,
                                    MatchingStatus status) {
        this.matchingIdx = matchingIdx;
        this.requestedAt = requestedAt;
        this.customerName = customerName;
        this.requestReason = requestReason;
        this.matchedAt = matchedAt;
        this.status = status;
    }

}
