package com.smhrd.dtect.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class LabelCount {
    private String label;
    private int count;
}