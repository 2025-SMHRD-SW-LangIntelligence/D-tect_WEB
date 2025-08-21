package com.smhrd.dtect.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OptionDto {
    private String code;   // enum
    private String label;  // 한글 라벨
}
