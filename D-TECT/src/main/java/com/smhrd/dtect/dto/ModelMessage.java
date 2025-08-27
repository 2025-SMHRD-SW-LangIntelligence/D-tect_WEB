package com.smhrd.dtect.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ModelMessage {
    private String user;    // 예: "3코빅수장아미" | "system"
    private String text;    // 예: "__FINALIZE_ONLY__"
    private String score;   // 예: "0.90"
    @JsonDeserialize(using = LabelCountListDeserializer.class)
    private List<LabelCount> classification; // 객체/배열 모두 허용
}
