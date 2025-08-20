package com.smhrd.dtect.dto;

import com.smhrd.dtect.entity.MatchingStatus;

import java.sql.Timestamp;

public class UserMatchingSummaryDto {

    private Long matchingIdx;
    private Timestamp requestedAt;
    private String lawyerName;
    private String requestReason;
    private Timestamp matchedAt;
    private MatchingStatus status;

    public UserMatchingSummaryDto(Long matchingIdx,
                                  Timestamp requestedAt,
                                  String lawyerName,
                                  String requestReason,
                                  Timestamp matchedAt,
                                  MatchingStatus status) {
        this.matchingIdx = matchingIdx;
        this.requestedAt = requestedAt;
        this.lawyerName = lawyerName;
        this.requestReason = requestReason;
        this.matchedAt = matchedAt;
        this.status = status;
    }

}
