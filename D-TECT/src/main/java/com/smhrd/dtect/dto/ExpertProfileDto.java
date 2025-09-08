package com.smhrd.dtect.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ExpertProfileDto {
    private Long expertId;
    private String name;
    private String email;
    private String officeName;
    private String officeAddress;

    private List<String> specialtyCodes;
    // 총 수익(포인트)
    private Long totalEarnings;
}
