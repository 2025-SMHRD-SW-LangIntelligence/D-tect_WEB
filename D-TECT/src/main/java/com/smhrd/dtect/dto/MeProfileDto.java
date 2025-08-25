package com.smhrd.dtect.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MeProfileDto {
    private String role;              // "USER" | "EXPERT"
    private Long memberId;

    private String name;
    private String email;

    // USER 전용
    private String address;

    // EXPERT 전용
    private String officeName;
    private String officeAddress;
    private List<String> specialtyCodes;
}
