package com.smhrd.dtect.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchingDetailDto {
    private Long   id;
    private String requestedAt;
    private String userName;
    private String message;
    private String reasonCode;
    private String reasonLabel;
    private String status;
    private String attachmentUrl;
}
